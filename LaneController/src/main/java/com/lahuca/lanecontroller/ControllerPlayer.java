/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 17:30 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LaneParty;

import java.util.Optional;
import java.util.UUID;

public class ControllerPlayer implements LanePlayer {

	private UUID uuid;
	private String name;
	private String displayName;
	private ControllerParty controllerParty;

	public ControllerPlayer(UUID uuid, String name, String displayName) {
		this.uuid = uuid;
		this.name = name;
		this.displayName = displayName;
	}

	@Override
	public UUID getUuid() {
		return uuid;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public Optional<LaneParty> getParty() {
		return Optional.ofNullable(controllerParty);
	}

}
