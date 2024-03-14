/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 12:48 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanegame;

import java.util.Set;
import java.util.UUID;

public class GameInfo {

	private UUID gameId;
	private String serverId;
	private String name;
	private GameState gameState;
	private Set<GamePlayer> players;

	public GameInfo(UUID gameId, String serverId, String name, GameState gameState, Set<GamePlayer> players) {
		this.gameId = gameId;
		this.serverId = serverId;
		this.name = name;
		this.gameState = gameState;
		this.players = players;
	}
}
