/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 19-3-2024 at 10:49 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.laneinstance;

import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.packet.GameStatusUpdatePacket;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The root endpoint for most calls of methods for a LaneInstance.
 */
public abstract class LaneInstance {

	private static LaneInstance instance;

	public static LaneInstance getInstance() {
		return instance;
	}

	private final Connection connection;
	private final HashMap<UUID, LaneGame> games = new HashMap<>();

	public LaneInstance(Connection connection) throws IOException {
		instance = this;
		this.connection = connection;
		connection.initialise(input -> {
			long requestId = input.requestId();
			requests.get(requestId).accept();
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

}
