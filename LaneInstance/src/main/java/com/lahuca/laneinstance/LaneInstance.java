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
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.request.*;
import com.lahuca.lane.records.PartyRecord;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lane.records.RelationshipRecord;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
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
    private boolean joinable;
    private boolean nonPlayable; // Tells whether the instance is also non playable: e.g. lobby

    public LaneInstance(Connection connection, boolean joinable, boolean nonPlayable) throws IOException {
        instance = this;
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
                        () -> players.put(record.uuid(), new InstancePlayer(record)));
                sendSimpleResult(packet, ResponsePacket.OK);
            } else if(input.packet() instanceof InstanceUpdatePlayerPacket packet) {
                PlayerRecord record = packet.playerRecord();
                getInstancePlayer(record.uuid()).ifPresentOrElse(
                        player -> player.applyRecord(record),
                        () -> players.put(record.uuid(), new InstancePlayer(record)));
                sendSimpleResult(packet, ResponsePacket.OK);
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

    private void sendController(Packet packet) {
        connection.sendPacket(packet, null);
    }

    private void sendSimpleResult(long requestId, String result) {
        sendController(new SimpleResultPacket(requestId, result));
    }

    private void sendSimpleResult(RequestPacket request, String result) {
        sendSimpleResult(request.getRequestId(), result);
    }

    private void sendInstanceStatus() {
        sendController(new InstanceStatusUpdatePacket(joinable, nonPlayable, getCurrentPlayers(), getMaxPlayers()));
    }

    public Optional<InstancePlayer> getInstancePlayer(UUID player) {
        return Optional.ofNullable(players.get(player));
    }

    public Optional<LaneGame> getInstanceGame(long gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    /**
     * This method is to be called when a player joins the instance.
     * This will transfer the player to the correct game, if applicable.
     *
     * @param uuid the player's uuid
     */
    public void joinInstance(UUID uuid) {
        getInstancePlayer(uuid).ifPresentOrElse(player -> player.getGameId().ifPresentOrElse(gameId -> getInstanceGame(gameId).ifPresentOrElse(game -> {
            // TODO Change the player's state
            game.onJoin(player);
        }, () -> {
            // TODO Hmm? Couldn't find the game with this ID on this instance? Report back to the controller
        }), () -> {
            // TODO Transfer player to the lobby of this instance, if it is joinable. Change the player's state!
        }), () -> {
            // TODO What odd? We have not received the packet with the information about the player.
            // TODO If is allowed by isJoinable and if slots left, move to instance lobby, otherwise send away.
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

    public CompletableFuture<RelationshipRecord> getRelationship(long relationshipId) {
        long id = System.currentTimeMillis();
        CompletableFuture<RelationshipRecord> completableFuture = buildFuture(id, o -> (RelationshipRecord) o); // TODO Maybe save the funciton somewhere, to save CPU?
        connection.sendPacket(new RelationshipPacket.Request(id, relationshipId), null);
        return completableFuture;
    }

    public CompletableFuture<PartyRecord> getParty(long partyId) {
        long id = System.currentTimeMillis();
        CompletableFuture<PartyRecord> completableFuture = buildFuture(id, o -> (PartyRecord) o); // TODO Maybe save the funciton somewhere, to save CPU?
        connection.sendPacket(new PartyPacket.Request(id, partyId), null);
        return completableFuture;
    }

}
