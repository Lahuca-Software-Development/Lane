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

public class Party {

	private final UUID owner;
	private final long creationTimestamp;
	private UUID[] players;

	public Party(UUID owner) {
		this.owner = owner;
		this.creationTimestamp = System.currentTimeMillis();
	}

	public UUID getOwner() {
		return owner;
	}

	public long getCreationTimestamp() {
		return creationTimestamp;
	}

}
