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

import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.LaneStateProperty;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.request.*;
import com.lahuca.lane.connection.socket.SocketConnectPacket;
import com.lahuca.lane.queue.QueueRequestParameters;
import com.lahuca.lane.records.PartyRecord;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lane.records.RelationshipRecord;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * The root endpoint for most calls of methods for a LaneInstance.
 */
public abstract class LaneInstance extends RequestHandler {

    private static LaneInstance instance;

    public static LaneInstance getInstance() {
        return instance;
    }

    private final Connection connection;
    private final HashMap<UUID, InstancePlayer> players = new HashMap<>();
    private final HashMap<Long, LaneGame> games = new HashMap<>();
    private String type;
    private boolean joinable;
    private boolean nonPlayable; // Tells whether the instance is also non-playable: e.g. lobby

    public LaneInstance(Connection connection, String type, boolean joinable, boolean nonPlayable) throws IOException, InstanceInstantiationException {
        if(instance != null) throw new InstanceInstantiationException();
        instance = this;
        this.type = type;

        Packet.registerPacket(GameStatusUpdatePacket.packetId, GameStatusUpdatePacket.class);
        Packet.registerPacket(InstanceDisconnectPacket.packetId, InstanceDisconnectPacket.class);
        Packet.registerPacket(InstanceJoinPacket.packetId, InstanceJoinPacket.class);
        Packet.registerPacket(InstanceStatusUpdatePacket.packetId, InstanceStatusUpdatePacket.class);
        Packet.registerPacket(InstanceUpdatePlayerPacket.packetId, InstanceUpdatePlayerPacket.class);
        Packet.registerPacket(PartyPacket.Player.Add.packetId, PartyPacket.Player.Add.class);
        Packet.registerPacket(PartyPacket.Player.Remove.packetId, PartyPacket.Player.Remove.class);
        Packet.registerPacket(PartyPacket.Disband.Request.packetId, PartyPacket.Disband.Request.class);
        Packet.registerPacket(PartyPacket.Retrieve.Request.packetId, PartyPacket.Retrieve.Request.class);
        Packet.registerPacket(PartyPacket.Retrieve.Response.packetId, PartyPacket.Retrieve.Response.class);
        Packet.registerPacket(QueueRequestPacket.packetId, QueueRequestPacket.class);
        Packet.registerPacket(RelationshipPacket.Create.Request.packetId, RelationshipPacket.Create.Request.class);
        Packet.registerPacket(RelationshipPacket.Retrieve.Request.packetId, RelationshipPacket.Retrieve.Request.class);
        Packet.registerPacket(RelationshipPacket.Retrieve.Response.packetId, RelationshipPacket.Retrieve.Response.class);
        Packet.registerPacket(SocketConnectPacket.packetId, SocketConnectPacket.class);
        Packet.registerPacket(SimpleResultPacket.packetId, SimpleResultPacket.class);

        this.connection = connection;
        this.joinable = joinable;
        this.nonPlayable = nonPlayable;
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
                getInstancePlayer(record.uuid()).ifPresentOrElse(
                        player -> player.applyRecord(record),
                        () -> players.put(record.uuid(), new InstancePlayer(record))); // TODO What happens if the player fails the connect?
                sendSimpleResult(packet, ResponsePacket.OK);
            } else if(input.packet() instanceof InstanceUpdatePlayerPacket packet) {
                PlayerRecord record = packet.playerRecord();
                getInstancePlayer(record.uuid()).ifPresentOrElse(
                        player -> player.applyRecord(record),
                        () -> players.put(record.uuid(), new InstancePlayer(record)));
            } else if(input.packet() instanceof ResponsePacket<?> response) {
                CompletableFuture<Result<?>> request = getRequests().get(response.getRequestId());
                if(request != null) {
                    // TODO How could it happen that the request is null?
                    request.complete(response.transformResult());
                    getRequests().remove(response.getRequestId());
                }
            }
        });
        sendInstanceStatus();
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
        sendController(new SimpleResultPacket(requestId, result));
    }

    private void sendSimpleResult(RequestPacket request, String result) {
        sendSimpleResult(request.getRequestId(), result);
    }

    private void sendInstanceStatus() {
        sendController(new InstanceStatusUpdatePacket(type, joinable, nonPlayable, getCurrentPlayers(), getMaxPlayers()));
    }

    public Optional<InstancePlayer> getInstancePlayer(UUID player) {
        return Optional.ofNullable(players.get(player));
    }

    public Optional<LaneGame> getInstanceGame(long gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    /**
     * Request the given player to be queued with the given parameters.
     * @param playerId the player's uuid
     * @param requestParameters the queue request parameters
     * @return The result of if the request has successfully be applied.
     */
    public CompletableFuture<Result<Void>> queue(UUID playerId, QueueRequestParameters requestParameters) {
        long id = System.currentTimeMillis();
        CompletableFuture<Result<Void>> completableFuture = buildVoidFuture(id);
        connection.sendPacket(new QueueRequestPacket(id, playerId, requestParameters), null);
        return completableFuture;
    }

    /**
     * This method is to be called when a player joins the instance.
     * This will transfer the player to the correct game, if applicable.
     *
     * @param uuid the player's uuid
     */
    public void joinInstance(UUID uuid) {
        System.out.println("Player: " + getInstancePlayer(uuid));
        getInstancePlayer(uuid).ifPresentOrElse(player -> player.getGameId().ifPresentOrElse(gameId -> getInstanceGame(gameId).ifPresentOrElse(game -> {
            // TODO Change the player's state
            String instanceId = player.getInstanceId().get(); // TODO WE SHOULD NOT DO THIS, SAVE THE ID in Instance rather than connection
            InstancePlayerState state = new InstancePlayerState(LanePlayerState.GAME_ONLINE,
                    Set.of(new StateProperty(LaneStateProperty.INSTANCE_ID, instanceId),
                            new StateProperty(LaneStateProperty.GAME_ID, game.getGameId()),
                            new StateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis()))); // TODO Better state handling!
            player.getState().applyRecord(state.convertRecord());
            sendController(new InstanceUpdatePlayerPacket(player.convertRecord()));
            game.onJoin(player);
        }, () -> {
            // Cannot find game, find new lobby. TODO Cancel join
            if(!isJoinable() || getCurrentPlayers() >= getMaxPlayers()) {
                // We cannot be at this instance.
                disconnectPlayer(uuid, "m"); // TODO Translate
                return;
            }
            // We can also join the instance instead, although we want to join a game.
            // TODO Join a game instead, game logic on controller.
            String instanceId = player.getInstanceId().get(); // TODO WE SHOULD NOT DO THIS, SAVE THE ID in Instance rather than connection
            InstancePlayerState state = new InstancePlayerState(LanePlayerState.INSTANCE_ONLINE,
                    Set.of(new StateProperty(LaneStateProperty.INSTANCE_ID, instanceId),
                            new StateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis()))); // TODO Better state handling!
            player.getState().applyRecord(state.convertRecord());
            sendController(new InstanceUpdatePlayerPacket(player.convertRecord()));
        }), () -> {
            // TODO Transfer player to the lobby of this instance, if it is joinable. Change the player's state!
            // Cannot find game, find new lobby. TODO Cancel join
            if(!isJoinable() || getCurrentPlayers() >= getMaxPlayers()) {
                // We cannot be at this instance.
                disconnectPlayer(uuid, "m"); // TODO Translate
                return;
            }
            // We can also join the instance instead, although we want to join a game.
            // TODO Join a game instead, game logic on controller.
            if(player.getInstanceId().isPresent()) {
                String instanceId = player.getInstanceId().get(); // TODO WE SHOULD NOT DO THIS, SAVE THE ID in Instance rather than connection
                InstancePlayerState state = new InstancePlayerState(LanePlayerState.INSTANCE_ONLINE,
                        Set.of(new StateProperty(LaneStateProperty.INSTANCE_ID, instanceId),
                                new StateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis()))); // TODO Better state handling!
                player.getState().applyRecord(state.convertRecord());
                sendController(new InstanceUpdatePlayerPacket(player.convertRecord()));
            }
        }), () -> {
            // Join the instance's lobby, if possible.
            // Cannot find game, find new lobby. TODO Cancel join
            if(!isJoinable() || getCurrentPlayers() >= getMaxPlayers()) {
                // We cannot be at this instance.
                disconnectPlayer(uuid, "m"); // TODO Translate
                return;
            }
            // We can also join the instance instead, although we want to join a game.
            // TODO Join a game instead, game logic on controller.
            String instanceId = null; // TODO WE SHOULD NOT DO THIS, SAVE THE ID in Instance rather than connection

            // TODO Fetch player first!

            InstancePlayerState state = new InstancePlayerState(LanePlayerState.INSTANCE_ONLINE,
                    Set.of(new StateProperty(LaneStateProperty.INSTANCE_ID, instanceId),
                            new StateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis()))); // TODO Better state handling!
            //player.getState().applyRecord(state.convertRecord());
            //sendController(new InstanceUpdatePlayerPacket(player.convertRecord()));
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

    public CompletableFuture<Result<Void>> registerGame(LaneGame game) {
        if(game == null) return simpleFuture(ResponsePacket.INVALID_PARAMETERS);
        if(games.containsKey(game.getGameId())) return simpleFuture(ResponsePacket.INVALID_ID);
        games.put(game.getGameId(), game);
        long requestId = getNewRequestId();
        CompletableFuture<Result<Void>> future = buildVoidFuture(requestId).thenApply(result -> {
            if(!result.isSuccessful()) {
                games.remove(game.getGameId());
            }
            return result;
        });
        connection.sendPacket(
                new GameStatusUpdatePacket(requestId, game.getGameId(), game.getName(), game.getGameState().convertRecord()), null);
        return future;
    }

    public CompletableFuture<Result<RelationshipRecord>> getRelationship(long relationshipId) {
        long id = System.currentTimeMillis();
        CompletableFuture<Result<RelationshipRecord>> completableFuture = buildFutureCast(id);
        connection.sendPacket(new RelationshipPacket.Retrieve.Request(id, relationshipId), null);
        return completableFuture;
    }

    public CompletableFuture<Result<PartyRecord>> getParty(long partyId) {
        long id = System.currentTimeMillis();
        CompletableFuture<Result<PartyRecord>> completableFuture = buildFutureCast(id); // TODO Maybe save the funciton somewhere, to save CPU?
        connection.sendPacket(new PartyPacket.Retrieve.Request(id, partyId), null);
        return completableFuture;
    }

    public CompletableFuture<Result<Void>> disbandParty(long partyId) {
        long id = System.currentTimeMillis();
        CompletableFuture<Result<Void>> completableFuture = buildVoidFuture(id);
        connection.sendPacket(new PartyPacket.Disband.Request(id, partyId), null);
        return completableFuture;
    }

    public CompletableFuture<Result<Void>> addPartyPlayer(long partyId, UUID player) {
        long id = System.currentTimeMillis();
        CompletableFuture<Result<Void>> completableFuture = buildVoidFuture(id);
        connection.sendPacket(new PartyPacket.Player.Add(id, partyId, player), null);
        return completableFuture;
    }

    public CompletableFuture<Result<Void>> removePartyPlayer(long partyId, UUID player) {
        long id = System.currentTimeMillis();
        CompletableFuture<Result<Void>> completableFuture = buildVoidFuture(id);
        connection.sendPacket(new PartyPacket.Player.Remove(id, partyId, player), null);
        return completableFuture;
    }

    public Collection<InstancePlayer> getPlayers() {
        return players.values(); // TODO REDO
    }

    public Collection<LaneGame> getGames() {
        return games.values();
    } // TODO Redo

}
