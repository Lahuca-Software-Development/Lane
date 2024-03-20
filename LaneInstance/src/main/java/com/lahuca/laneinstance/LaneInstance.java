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
import com.lahuca.lane.connection.packet.PartyPacket;
import com.lahuca.lane.connection.packet.RelationshipPacket;
import com.lahuca.lane.records.PartyRecord;
import com.lahuca.lane.records.RelationshipRecord;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The root endpoint for most calls of methods for a LaneInstance.
 */
public abstract class LaneInstance {

	private static LaneInstance instance;

	public static LaneInstance getInstance() {
		return instance;
	}

	private final Connection connection;
	private final HashMap<Long, LaneGame> games = new HashMap<>();
    private final HashMap<Long, CompletableFuture<?>> requestablePackets = new HashMap<>();

    public LaneInstance(Connection connection) throws IOException {
		instance = this;
		this.connection = connection;
		connection.initialise(input -> {
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

	public void registerGame(LaneGame game) {
		if(games.containsKey(game.getGameId())) return; // TODO Already a game with said id.
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
