/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 13-3-2024 at 21:31 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller;

import com.lahuca.lane.Party;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * This is the main class for operations on the controller side of the Lane system.
 */
public class Controller {

	private final Set<ControllerPlayer> players = new HashSet<>();
	private final Set<ControllerGame> games = new HashSet<>();

	public void registerGame(ControllerGame controllerGame) {
		games.add(controllerGame);
	}

	public void endGame(ControllerGame controllerGame) {

	}

	public void joinPlayer(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {
		players.add(controllerPlayer);
	}

	public void partyWarp(Party party, ControllerGame controllerGame) {

	}

	public void spectateGame(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {

	}

	public Optional<ControllerPlayer> getPlayer(UUID uuid) {
		return players.stream().filter(player -> player.getUuid().equals(uuid)).findFirst();
	}

	public Optional<ControllerGame> getGame(UUID uuid) {
		return games.stream().filter(game -> game.getGameId().equals(uuid)).findFirst();
	}

	public Set<ControllerPlayer> getPlayers() {
		return players;
	}
}
