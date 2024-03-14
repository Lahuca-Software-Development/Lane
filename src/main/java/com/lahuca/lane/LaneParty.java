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

import java.util.Set;
import java.util.UUID;

public interface LaneParty {

	/**
	 * Gets owner of this party.
	 *
	 * @return The owner's uuid of this party
	 */
	UUID getOwner();

	/**
	 * Gets uuids of users in this party
	 *
	 * @return All users uuid
	 */
	Set<UUID> getPlayers();

	/**
	 * Gets uuids of requested players to this party
	 *
	 * @return All requested users uuid
	 */
	Set<UUID> getRequested();


	/**
	 * Gets a time stamp when this party was created
	 *
	 * @return The time stamp where was this party created
	 */
	long getCreationTimestamp();

}
