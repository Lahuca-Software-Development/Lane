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
package com.lahuca.laneinstance;

import java.util.HashSet;
import java.util.UUID;

public class GameInfo {

	private final long gameId; // TODO Really long?
	private String name;
	private GameState gameState;
	private final HashSet<UUID> players = new HashSet<>();

	public GameInfo(long gameId, String name, GameState gameState) {
		this.gameId = gameId;
		this.name = name;
		this.gameState = gameState;
	}

	public long getGameId() {
		return gameId;
	}

	public String getName() {
		return name;
	}

	public void setGameState(GameState gameState) {
		this.gameState = gameState;
	}

	public GameState getGameState() {
		return gameState;
	}

}