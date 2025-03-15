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

import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.ReconnectConnection;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.request.*;
import com.lahuca.lane.queue.QueueRequestParameters;
import com.lahuca.lane.records.PartyRecord;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lane.records.RelationshipRecord;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * The root endpoint for most calls of methods for a LaneInstance.
 */
public abstract class LaneInstance {

    private static LaneInstance instance;

    public static LaneInstance getInstance() {
        return instance;
    }

    private final String id;
    private final ReconnectConnection connection;
    private final HashMap<UUID, InstancePlayer> players = new HashMap<>();
    private final HashMap<Long, LaneGame> games = new HashMap<>();
    private String type;
    private boolean joinable;
    private boolean nonPlayable; // Tells whether the instance is also non-playable: e.g. lobby

    public LaneInstance(String id, ReconnectConnection connection, String type, boolean joinable, boolean nonPlayable) throws IOException, InstanceInstantiationException {
        if(instance != null) throw new InstanceInstantiationException();
        instance = this;
        this.id = id;
        this.type = type;

        Packet.registerPackets();

        this.connection = connection;
        this.joinable = joinable;
        this.nonPlayable = nonPlayable;
        connection.setOnReconnect(this::sendInstanceStatus);
        connection.initialise(input -> {
            if(input.packet() instanceof InstanceJoinPacket packet) {
                if(!isJoinable()) {
                    sendSimpleResult(packet, ResponsePacket.NOT_JOINABLE);
                    return;
                }
                // TODO Find if slot, also when the max players which has been RESERVED is met
                // TODO Added later at 15 June, it looks like this already accounts for reserved positions.
                if(!packet.overrideSlots() && getCurrentPlayers() >= getMaxPlayers()) {
                    sendSimpleResult(packet, ResponsePacket.NO_FREE_SLOTS);
                    return;
                }
                if(packet.gameId() != null) {
                    Optional<LaneGame> instanceGame = getInstanceGame(packet.gameId());
                    if(instanceGame.isEmpty()) {
                        sendSimpleResult(packet, ResponsePacket.INVALID_ID);
                        return;
                    }
                    LaneGame game = instanceGame.get();
                    GameState state = game.getGameState();
                    if(!state.isJoinable()) {
                        sendSimpleResult(packet, ResponsePacket.NOT_JOINABLE);
                        return;
                    }
                    // TODO What about max players in a game?
                }
                // We are here, so we can apply it.
                PlayerRecord record = packet.player();
                getInstancePlayer(record.uuid()).ifPresentOrElse(player -> player.applyRecord(record),
                        () -> players.put(record.uuid(), new InstancePlayer(record))); // TODO What happens if the player fails the connect?
                sendSimpleResult(packet, ResponsePacket.OK);
            } else if(input.packet() instanceof InstanceUpdatePlayerPacket packet) {
                PlayerRecord record = packet.playerRecord();
                getInstancePlayer(record.uuid()).ifPresent(player -> player.applyRecord(record));
            } else if(input.packet() instanceof ResponsePacket<?> response) {
                if(!connection.retrieveResponse(response.getRequestId(), response.transformResult())) {
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

    private Connection getConnection() {
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
        joinable = joinable;
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

    public abstract void disconnectPlayer(UUID player, String message);

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
     * @param result The result.
     * @return A CompletableFuture consisting of no additional data and the result.
     */
    public static CompletableFuture<Result<Void>> simpleFuture(String result) {
        return CompletableFuture.completedFuture(new Result<>(result));
    }

    public static <T> Request<T> simpleRequest(String result) {
        return new Request<>(new Result<>(result));
    }

    private void sendInstanceStatus() {
        sendController(new InstanceStatusUpdatePacket(type, joinable, nonPlayable, getCurrentPlayers(),
                getMaxPlayers()));
    }

    public Optional<InstancePlayer> getInstancePlayer(UUID player) {
        return Optional.ofNullable(players.get(player));
    }

    public Optional<LaneGame> getInstanceGame(long gameId) {
        return Optional.ofNullable(games.get(gameId));
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

    /**
     * This method is to be called when a player joins the instance.
     * This will transfer the player to the correct game, if applicable.
     *
     * @param uuid the player's uuid
     */
    public void joinInstance(UUID uuid) {
        // TODO When we disconnect, maybe not always display message? Where would it be put? Chat?
        getInstancePlayer(uuid).ifPresentOrElse(player -> {
            // Okay, we should allow the join.
            player.getGameId().ifPresentOrElse(gameId -> {
                // Player tries to join game
                getInstanceGame(gameId).ifPresentOrElse(game -> {
                    // Player tries to join this instance. Check if possible.
                    if(!isJoinable() || getCurrentPlayers() >= getMaxPlayers()) {
                        // We cannot be at this instance.
                        disconnectPlayer(uuid, "Instance not joinable or full"); // TODO Translate
                        return;
                    }
                    // TODO Maybe game also has slots? Or limitations?
                    // Send queue finished to controller
                    try {
                        Result<Void> result = connection.<Void>sendRequestPacket(id -> new QueueFinishedPacket(id, uuid, gameId), null).getFutureResult().get();
                        if(result == null || !result.isSuccessful()) {
                            disconnectPlayer(uuid, "Queue not finished"); // TODO Translate
                            return;
                        }
                        sendInstanceStatus();
                        game.onJoin(player);
                    } catch (InterruptedException | ExecutionException | CancellationException e) {
                        disconnectPlayer(uuid, "Could not process queue"); // TODO Translate
                    }
                }, () -> {
                    // The given game ID does not exist on this instance. Disconnect
                    disconnectPlayer(uuid, "Invalid game ID on instance."); // TODO Translateable
                });
            }, () -> {
                // Player tries to join this instance. Check if possible.
                if(!isJoinable() || getCurrentPlayers() >= getMaxPlayers()) {
                    // We cannot be at this instance.
                    disconnectPlayer(uuid, "Instance not joinable or full"); // TODO Translate
                    return;
                }
                // Join allowed, finalize
                try {
                    Result<Void> result = connection.<Void>sendRequestPacket(id -> new QueueFinishedPacket(id, uuid, null), null).getFutureResult().get();
                    if(!result.isSuccessful()) {
                        disconnectPlayer(uuid, "Queue not finished"); // TODO Translate
                        return;
                    }
                    sendInstanceStatus();
                    // TODO Handle, like TP, etc. Only after response.
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    disconnectPlayer(uuid, "Could not process queue"); // TODO Translate
                }
            });
        }, () -> {
            // We do not have the details about this player. Controller did not send it.
            // Disconnect player, as we are unaware if this is correct.
            disconnectPlayer(uuid, "Incorrect state."); // TODO Translateable
        });
    }

    /**
     * Sets that the given player is leaving the current server, only works for players on this instance.
     * If the player is on this server, will remove its data and call the correct functions.
     *
     * @param uuid
     */
    public void quitInstance(UUID uuid) {
        getInstancePlayer(uuid).ifPresent(player -> quitGame(uuid));
        players.remove(uuid);
        sendInstanceStatus();
    }

    /**
     * Sets that the given player is quitting its game, only works for players on this instance.
     *
     * @param uuid the player's uuid
     */
    public void quitGame(UUID uuid) {
        getInstancePlayer(uuid).ifPresent(player -> player.getGameId().flatMap(this::getInstanceGame).ifPresent(game -> {
            game.onQuit(player);
            // TODO Remove player actually from player list in the game
        }));
    }

    private Request<Long> requestId(RequestIdPacket.Type idType) {
        if(idType == null) return simpleRequest(ResponsePacket.INVALID_PARAMETERS);
        return connection.sendRequestPacket(id -> new RequestIdPacket(id, idType), null);
    }

    public Request<Void> registerGame(Function<Long, LaneGame> gameConstructor) {
        if(gameConstructor == null) return simpleRequest(ResponsePacket.INVALID_PARAMETERS);
        try {
            Result<Long> gameId = requestId(RequestIdPacket.Type.GAME).getFutureResult().get();
            if(!gameId.isSuccessful()) {
                return simpleRequest(ResponsePacket.INVALID_STATE);
            }
            LaneGame game = gameConstructor.apply(gameId.data());
            if(games.containsKey(game.getGameId())) return simpleRequest(ResponsePacket.INVALID_ID);
            games.put(game.getGameId(), game);
            Request<Void> request = connection.sendRequestPacket(id -> new GameStatusUpdatePacket(id, game.getGameId(), game.getName(),
                    game.getGameState().convertRecord()), null);
            return request.thenApply(result -> {
                if(!result.isSuccessful()) {
                    games.remove(game.getGameId());
                }
                return result;
            });
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            return simpleRequest(ResponsePacket.INTERRUPTED);
        }
    }

    public Request<RelationshipRecord> getRelationship(long relationshipId) {
        return connection.sendRequestPacket(id -> new RelationshipPacket.Retrieve.Request(id, relationshipId), null);
    }

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

    public Collection<InstancePlayer> getPlayers() {
        return players.values(); // TODO REDO
    }

    public Collection<LaneGame> getGames() {
        return games.values();
    } // TODO Redo

}
