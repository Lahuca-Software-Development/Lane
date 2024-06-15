/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 12:52 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane;

import com.lahuca.lane.records.StatePropertyRecord;

/**
 * Interface representing a game state property.
 */
public interface LaneStateProperty extends RecordApplier<StatePropertyRecord> {

	/**
	 * Constant for the instance ID property.
	 */
	String INSTANCE_ID = "instanceId";
	/**
	 * Constant for the game ID property.
	 */
	String GAME_ID = "gameId";
	/**
	 * Constant for the timestamp property.
	 */
	String TIMESTAMP = "timestamp";

	/**
	 * Gets the ID of this property.
	 *
	 * @return the ID of this property
	 */
	String getId();

	/**
	 * Gets the value of this property.
	 *
	 * @return the value of this property
	 */
	Object getValue();

	/**
	 * Gets the extra data associated with this property.
	 *
	 * @return the extra data associated with this property
	 */
	Object getExtraData();


}
