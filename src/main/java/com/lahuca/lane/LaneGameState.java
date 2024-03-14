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

/**
 * Interface representing a game state.
 */
public interface LaneGameState {

	/**
	 * Gets the name of this game state.
	 *
	 * @return the name of this game state
	 */
	String getName();

	/**
	 * Checks if this game state is joinable.
	 *
	 * @return true if this game state is joinable, false otherwise
	 */
	boolean isJoinable();

	/**
	 * Checks if this game state is playable.
	 *
	 * @return true if this game state is playable, false otherwise
	 */
	boolean isPlayable();

	/**
	 * Gets the properties associated with this game state.
	 *
	 * @return the properties associated with this game state
	 */
	HashMap<String, ? extends LaneStateProperty> getProperties();

}
