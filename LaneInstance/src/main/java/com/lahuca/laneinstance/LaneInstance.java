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
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.request.RequestHandler;
import com.lahuca.lane.connection.request.ResponsePacket;
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
            if(input.packet() instanceof InstanceJoinPacket join) {
                // TODO Also check if the instance is joinable
                if(!games.containsKey(join.gameId())) {
                    // TODO What to do now? The game they are being transferred to, is not active on this instance
                }
                for(PlayerRecord record : join.players()) {
                    getInstancePlayer(record.uuid()).ifPresentOrElse(
                            player -> player.applyRecord(record),
                            () -> players.put(record.uuid(), new InstancePlayer(record)));
                }
            } else if(input.packet() instanceof ResponsePacket<?> response) {
                CompletableFuture<Object> request = getRequests().get(response.getRequestId());
                if(request != null) {
                    // TODO How could it happen that the request is null?
                    request.complete(response.getData());
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

    private void sendInstanceStatus() {
        connection.sendPacket(new InstanceStatusUpdatePacket(joinable, nonPlayable), null);
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
        });
    }

	public void registerGame(LaneGame game) {
		if(games.containsKey(game.getGameId())) return; // TODO Already a game with said id on this server.
        // TODO Check whether there is a game on the controller with the given ID.
		games.put(game.getGameId(), game);
		connection.sendPacket(
				new GameStatusUpdatePacket(game.getGameId(), game.getName(), game.getGameState().convertRecord()), null);
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
