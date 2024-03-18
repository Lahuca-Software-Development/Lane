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

import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.GameStatusUpdatePacket;

import java.io.IOException;
import java.util.*;

/**
 * This is the main class for operations on the controller side of the Lane system.
 */
public class Controller {

	private static Controller instance;

	public static Controller getInstance() {
		return instance;
	}

	private final Connection connection;

	private final Set<ControllerPlayer> players;
	private final HashMap<UUID, ControllerGame> games;

	public Controller(Connection connection) throws IOException {
		instance = this;
		players = new HashSet<>();
		games = new HashMap<>();

		this.connection = connection;

		connection.initialise(input -> {
			Packet packet = input.packet();
			if(packet instanceof GameStatusUpdatePacket gameStatusUpdate) {
				// TODO
			}
		});
	}

	public Connection getConnection() {
		return connection;
	}

	public void registerGame(ControllerGame controllerGame) {
		games.put(controllerGame.getGameId(), controllerGame);
	}

	public void endGame(ControllerGame controllerGame) {
		endGame(controllerGame.getGameId());
	}

	public void endGame(UUID uuid) {
		games.remove(uuid);
	}

	public void joinPlayer(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {
		players.add(controllerPlayer);
	}

	public void leavePlayer(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {
		players.remove(controllerPlayer);
	}


	public void partyWarp(ControllerParty controllerParty, ControllerGame controllerGame) {

	}

	public void spectateGame(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {
		controllerPlayer.setPlayerState(new ControllerPlayerState("Spectating", new HashMap<>()));
	}

	public Optional<ControllerPlayer> getPlayerByName(String name) {
		return players.stream().filter(player -> player.getName().equals(name)).findFirst();
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

	public Set<ControllerGame> getGames() {
		return games;
	}


}
