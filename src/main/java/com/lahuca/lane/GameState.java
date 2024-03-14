/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 12:50 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a game state with properties.
 */
public record GameState(String name, boolean isJoinable, boolean isPlayable, Map<String, GameStateProperty> properties) {


	/**
	 * Constructs a new GameState with the specified name, joinable, and spectatable flags.
	 *
	 * @param name         the name of this game state
	 * @param isJoinable   flag indicating if this game state is joinable
	 * @param isPlayable flag indicating if this game state is playable
	 */
	public GameState(String name, boolean isJoinable, boolean isPlayable) {
		this(name, isJoinable, isPlayable, new HashMap<>());
	}

	/**
	 * Gets the name of this game state.
	 *
	 * @return the name of this game state
	 */
	public String getName() {
		return name;
	}

	/**
	 * Checks if this game state is joinable.
	 *
	 * @return true if this game state is joinable, false otherwise
	 */
	public boolean isJoinable() {
		return isJoinable;
	}

	/**
	 * Checks if this game state is playable.
	 *
	 * @return true if this game is playable, false otherwise
	 */
	public boolean isPlayable() {
		return isPlayable;
	}

	/**
	 * Gets the properties associated with this game state.
	 *
	 * @return the properties associated with this game state
	 */
	public Map<String, GameStateProperty> getProperties() {
		return properties;
	}

}
