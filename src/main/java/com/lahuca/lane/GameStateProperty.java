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

/**
 * This class contains information about any additional properties that are set in the game state.
 * These properties can be game instance dependent and are expected to be non-existent at any time.
 */
public class GameStateProperty {

	/** The unique identifier for this property. */
	private final String id;

	/** The value of this property. */
	private Object value;

	/** Additional data associated with this property. */
	private Object extraData;

	/**
	 * Constructs a new GameStateProperty with the specified ID.
	 *
	 * @param id the unique identifier for this property
	 */
	public GameStateProperty(String id) {
		this.id = id;
	}

	/**
	 * Constructs a new GameStateProperty with the specified ID and value.
	 *
	 * @param id    the unique identifier for this property
	 * @param value the initial value for this property
	 */
	public GameStateProperty(String id, Object value) {
		this.id = id;
		this.value = value;
	}

	/**
	 * Constructs a new GameStateProperty with the specified ID, value, and extra data.
	 *
	 * @param id        the unique identifier for this property
	 * @param value     the initial value for this property
	 * @param extraData additional data associated with this property
	 */
	public GameStateProperty(String id, Object value, Object extraData) {
		this.id = id;
		this.value = value;
		this.extraData = extraData;
	}

	/**
	 * Gets the ID of this property.
	 *
	 * @return the ID of this property
	 */
	public String getId() {
		return id;
	}

	/**
	 * Gets the value of this property.
	 *
	 * @return the value of this property
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Sets the value of this property.
	 *
	 * @param value the new value for this property
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Gets the extra data associated with this property.
	 *
	 * @return the extra data associated with this property
	 */
	public Object getExtraData() {
		return extraData;
	}

	/**
	 * Sets the extra data associated with this property.
	 *
	 * @param extraData the new extra data for this property
	 */
	public void setExtraData(Object extraData) {
		this.extraData = extraData;
	}

}
