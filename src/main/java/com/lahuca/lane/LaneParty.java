/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 12:38 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane;

import java.util.UUID;

public interface LaneParty extends LaneRelationship {


	/**
	 * Gets party id.
	 *
	 * @return The party id
	 */
	long getPartyId();


	/**
	 * Gets owner of this party.
	 *
	 * @return The owner's uuid of this party
	 */
	UUID getOwner();

	/**
	 * Gets a time stamp when this party was created
	 *
	 * @return The time stamp where was this party created
	 */
	long getCreationTimestamp();

}
