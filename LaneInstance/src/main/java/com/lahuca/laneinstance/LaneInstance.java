/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 18-3-2024 at 14:43 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.laneinstance;

import com.lahuca.lane.ReconnectConnection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.packet.data.DataObjectReadPacket;
import com.lahuca.lane.connection.packet.data.DataObjectRemovePacket;
import com.lahuca.lane.connection.packet.data.DataObjectWritePacket;
import com.lahuca.lane.connection.request.Request;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.Result;
import com.lahuca.lane.connection.request.result.VoidResultPacket;
import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.queue.QueueRequestParameters;
import com.lahuca.lane.records.*;
import com.lahuca.laneinstance.managers.InstancePlayerManager;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * The root endpoint for most calls of methods for a LaneInstance.
 */
public abstract class LaneInstance implements RecordConverter<InstanceRecord> {

    private static LaneInstance instance;

    public static LaneInstance getInstance() { // TODO Optional?
        return instance;
    }

    private final String id;
    private final ReconnectConnection connection;
    private String type;
    private boolean joinable;
    private boolean nonPlayable; // Tells whether the instance is also non-playable: e.g. lobby

    private final InstancePlayerManager playerManager;

    private final HashMap<Long, LaneGame> games = new HashMap<>();

    public LaneInstance(String id, ReconnectConnection connection, String type, boolean joinable, boolean nonPlayable) throws IOException, InstanceInstantiationException {
        if (instance != null) throw new InstanceInstantiationException();
        instance = this;
        this.id = id;
        this.type = type;

        Packet.registerPackets();

        this.connection = connection;
        this.joinable = joinable;
        this.nonPlayable = nonPlayable;

        playerManager = new InstancePlayerManager(this, this::sendInstanceStatus);

        connection.setOnReconnect(this::sendInstanceStatus);
        connection.initialise(input -> {
            if (input.packet() instanceof InstanceJoinPacket packet) {
                if (!isJoinable()) {
                    sendSimpleResult(packet, ResponsePacket.NOT_JOINABLE);
                    return;
                }
                // TODO Find if slot, also when the max players which has been RESERVED is met
                // TODO Added later at 15 June, it looks like this already accounts for reserved positions.
                if (!packet.overrideSlots() && getCurrentPlayers() >= getMaxPlayers()) {
                    sendSimpleResult(packet, ResponsePacket.NO_FREE_SLOTS);
                    return;
                }
                if (packet.gameId() != null) {
                    Optional<LaneGame> instanceGame = getInstanceGame(packet.gameId());
                    if (instanceGame.isEmpty()) {
                        sendSimpleResult(packet, ResponsePacket.INVALID_ID);
                        return;
                    }
                    LaneGame game = instanceGame.get();
                    GameState state = game.getGameState();
                    if (!state.isJoinable()) {
                        sendSimpleResult(packet, ResponsePacket.NOT_JOINABLE);
                        return;
                    }
                    // TODO What about max players in a game?
                }
                // We are here, so we can apply it.
                getPlayerManager().registerPlayer(packet.player());
                sendSimpleResult(packet, ResponsePacket.OK);
            } else if (input.packet() instanceof InstanceUpdatePlayerPacket packet) {
                PlayerRecord record = packet.playerRecord();
                getPlayerManager().getInstancePlayer(record.uuid()).ifPresent(player -> player.applyRecord(record));
            } else if (input.packet() instanceof ResponsePacket<?> response) {
                if (!connection.retrieveResponse(response.getRequestId(), response.transformResult())) {
                    // TODO Handle output: failed response
                }
            }
        });
        sendInstanceStatus();
    }

    public void shutdown() {
        connection.disableReconnect();
        connection.close();
        // TODO Probably other stuff?
    }

    public ReconnectConnection getConnection() {
        return connection;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        sendInstanceStatus();
    }

    public boolean isJoinable() {
        return joinable;
    }

    public void setJoinable(boolean joinable) {
        this.joinable = joinable;
        sendInstanceStatus();
    }

    public boolean isNonPlayable() {
        return nonPlayable;
    }

    public void setNonPlayable(boolean nonPlayable) {
        this.nonPlayable = nonPlayable;
        sendInstanceStatus();
    }

    public abstract int getCurrentPlayers();

    public abstract int getMaxPlayers();

    public InstancePlayerManager getPlayerManager() {
        return playerManager;
    }

    public abstract void disconnectPlayer(UUID player, Component message);

    public void sendController(Packet packet) {
        connection.sendPacket(packet, null);
    }

    private void sendSimpleResult(long requestId, String result) {
        sendController(new VoidResultPacket(requestId, result));
    }

    private void sendSimpleResult(RequestPacket request, String result) {
        sendSimpleResult(request.getRequestId(), result);
    }

    /**
     * Constructs a CompletableFuture that contains the direct result.
     *
     * @param result The result.
     * @return A CompletableFuture consisting of no additional data and the result.
     */
    private static CompletableFuture<Result<Void>> simpleFuture(String result) { // TODO Move, it is also
        return CompletableFuture.completedFuture(new Result<>(result));
    }

    private static <T> Request<T> simpleRequest(String result) { // TODO Move
        return new Request<>(new Result<>(result));
    }

    private void sendInstanceStatus() {
        sendController(new InstanceStatusUpdatePacket(convertRecord()));
    }

    /**
     * Request the given player to be queued with the given parameters.
     *
     * @param playerId          the player's uuid
     * @param requestParameters the queue request parameters
     * @return The request
     */
    public Request<Void> queue(UUID playerId, QueueRequestParameters requestParameters) {
        return connection.sendRequestPacket(id -> new QueueRequestPacket(id, playerId, requestParameters), null);
    }

    private Request<Long> requestId(RequestIdPacket.Type idType) {
        if (idType == null) return simpleRequest(ResponsePacket.INVALID_PARAMETERS);
        return connection.sendRequestPacket(id -> new RequestIdPacket(id, idType), null);
    }

    /**
     * Registers a new game upon the function that gives a new game ID.
     *
     * @param gameConstructor the id to game parser. Preferably a lambda with LaneGame::new is given, whose constructor consists of only the ID.
     * @return the request of the registering, it completes successfully with the game when it is successfully registered.
     */
    public Request<LaneGame> registerGame(Function<Long, LaneGame> gameConstructor) {
        if (gameConstructor == null) return simpleRequest(ResponsePacket.INVALID_PARAMETERS);
        try {
            Result<Long> gameId = requestId(RequestIdPacket.Type.GAME).getFutureResult().get();
            if (!gameId.isSuccessful()) {
                return simpleRequest(ResponsePacket.ILLEGAL_STATE);
            }
            LaneGame game = gameConstructor.apply(gameId.data());
            if (game.getGameId() != gameId.data() || games.containsKey(game.getGameId()))
                return simpleRequest(ResponsePacket.INVALID_ID);
            games.put(game.getGameId(), game);
            Request<Void> request = connection.sendRequestPacket(id -> new GameStatusUpdatePacket(id, game.getGameId(), game.getName(),
                    game.getGameState().convertRecord()), null);
            return request.thenApplyConstruct(result -> {
                if (!result.isSuccessful()) {
                    games.remove(game.getGameId());
                    return new Result<>(result.result(), null);
                }
                return new Result<>(result.result(), game);
            });
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            return simpleRequest(ResponsePacket.INTERRUPTED); // TODO Forward to Request?
        }
    }

    /**
     * Reads a data object at the given id with the provided permission key.
     *
     * @param id            the id of the data object
     * @param permissionKey the permission key that wants to retrieve the data object, this must be an individual key
     * @return the request with the data object; the data object is null when there is no data object at the id.
     */
    public Request<DataObject> readDataObject(DataObjectId id, PermissionKey permissionKey) {
        if (id == null || permissionKey == null || !permissionKey.isFormattedCorrectly())
            return simpleRequest(ResponsePacket.INVALID_PARAMETERS);
        return connection.sendRequestPacket(requestId -> new DataObjectReadPacket(requestId, id, permissionKey), null);
    }

    /**
     * Writes a data object at the given id with the provided permission key.
     * This either creates or updates the data object.
     *
     * @param object        the id of the data object
     * @param permissionKey the permission key that wants to write the data object, this must be an individual key
     * @return the request with the status
     */
    public Request<Void> writeDataObject(DataObject object, PermissionKey permissionKey) {
        if (object == null || permissionKey == null || !permissionKey.isFormattedCorrectly())
            return simpleRequest(ResponsePacket.INVALID_PARAMETERS);
        return connection.sendRequestPacket(requestId -> new DataObjectWritePacket(requestId, object, permissionKey), null);
    }

    /**
     * Removes a data object at the given id with the provided permission key.
     *
     * @param id            the id of the data object
     * @param permissionKey the permission key that wants to remove the data object, this must be an individual key
     * @return the request with the status
     */
    public Request<Void> removeDataObject(DataObjectId id, PermissionKey permissionKey) {
        if (id == null || permissionKey == null || !permissionKey.isFormattedCorrectly())
            return simpleRequest(ResponsePacket.INVALID_PARAMETERS);
        return connection.sendRequestPacket(requestId -> new DataObjectRemovePacket(requestId, id, permissionKey), null);
    }

    // TODO updateDataObject
    // TODO Maybe still delagate getInstancePLayer()?

    /**
     * Retrieves an InstanceGame by a given game ID on this instance.
     *
     * @param gameId the game ID of the game
     * @return an optional of the LaneGame object.
     */
    public Optional<LaneGame> getInstanceGame(long gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    /**
     * Retrieves a collection of all Lane games on this instance.
     *
     * @return the collection
     */
    public Collection<LaneGame> getInstanceGames() {
        return games.values();
    }

    /**
     * Retrieves a request of the game record of the game with the given ID on the controller.
     * The value is null when no game with the given ID is present.
     * @param gameId the ID of the game
     * @return the request
     */
    public Request<PlayerRecord> getGameRecord(long gameId) {
        return connection.sendRequestPacket(id -> new RequestInformationPacket.Game(id, gameId), null);
    }

    /**
     * Retrieves a request of a collection of immutable records of all games on the controller.
     *
     * @return the request
     */
    public Request<ArrayList<GameRecord>> getAllGameRecords() {
        return connection.sendRequestPacket(RequestInformationPacket.Games::new, null);
    }

    /**
     * Retrieves a request of the instance record of the instance with the given ID on the controller.
     * The value is null when no instance with the given ID is present.
     * @param id the ID of the instance
     * @return the request
     */
    public Request<InstanceRecord> getInstanceRecord(String id) {
        if(id == null || id.isEmpty()) return simpleRequest(ResponsePacket.INVALID_PARAMETERS);
        return connection.sendRequestPacket(requestId -> new RequestInformationPacket.Instance(requestId, id), null);
    }

    /**
     * Retrieves a request of a collection of immutable records of all instances on the controller.
     *
     * @return the request
     */
    public Request<ArrayList<InstanceRecord>> getAllInstanceRecords() {
        return connection.sendRequestPacket(RequestInformationPacket.Instances::new, null);
    }

    @Override
    public InstanceRecord convertRecord() {
        return new InstanceRecord(id, type, joinable, nonPlayable, getCurrentPlayers(), getMaxPlayers());
    }

    // TODO Below: todo

    public Request<PartyRecord> getParty(long partyId) {
        return connection.sendRequestPacket(id -> new PartyPacket.Retrieve.Request(id, partyId), null);
    }

    public Request<Void> disbandParty(long partyId) {
        return connection.sendRequestPacket(id -> new PartyPacket.Disband.Request(id, partyId), null);
    }

    public Request<Void> addPartyPlayer(long partyId, UUID player) {
        return connection.sendRequestPacket(id -> new PartyPacket.Player.Add(id, partyId, player), null);
    }

    public Request<Void> removePartyPlayer(long partyId, UUID player) {
        return connection.sendRequestPacket(id -> new PartyPacket.Player.Remove(id, partyId, player), null);
    }

}
