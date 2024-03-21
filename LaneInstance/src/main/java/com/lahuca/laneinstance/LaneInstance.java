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
import com.lahuca.lane.connection.packet.GameStatusUpdatePacket;
import com.lahuca.lane.connection.packet.InstanceJoinPacket;
import com.lahuca.lane.connection.packet.PartyPacket;
import com.lahuca.lane.connection.packet.RelationshipPacket;
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
public abstract class LaneInstance {

	private static LaneInstance instance;

	public static LaneInstance getInstance() {
		return instance;
	}

	private final Connection connection;
    private final HashMap<UUID, InstancePlayer> players = new HashMap<>();
	private final HashMap<Long, LaneGame> games = new HashMap<>();
    private final HashMap<Long, CompletableFuture<?>> requestablePackets = new HashMap<>();

    public LaneInstance(Connection connection) throws IOException {
		instance = this;
		this.connection = connection;
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
            }

            if(input.packet() instanceof PartyPacket.Response responsePacket) {
                CompletableFuture<PartyRecord> future = (CompletableFuture<PartyRecord>) requestablePackets.get(responsePacket.getRequestId());
                future.complete(responsePacket.getData());
            } else if(input.packet() instanceof RelationshipPacket.Response responsePacket) {
                CompletableFuture<RelationshipRecord> future = (CompletableFuture<RelationshipRecord>) requestablePackets.get(responsePacket.getRequestId());
                future.complete(responsePacket.getData());
            }
		});
	}

	private Connection getConnection() {
		return connection;
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

        CompletableFuture<RelationshipRecord> completableFuture = new CompletableFuture<>();
        requestablePackets.put(id, completableFuture);

        connection.sendPacket(new RelationshipPacket.Request(id, relationshipId), null);
        return completableFuture;
    }

    public CompletableFuture<PartyRecord> getParty(long partyId) {
        long id = System.currentTimeMillis();

        CompletableFuture<PartyRecord> completableFuture = new CompletableFuture<>();
        requestablePackets.put(id, completableFuture);

        connection.sendPacket(new PartyPacket.Request(id, partyId), null);
        return completableFuture;
    }
}
