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
import com.lahuca.lane.connection.packet.PlayerJoinGamePacket;
import com.lahuca.lane.connection.packet.PlayerQuitGamePacket;
import com.lahuca.lane.connection.packet.RequestPartyPacket;

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
	private final HashMap<UUID, ControllerGame> games = new HashMap<>();

	public Controller(Connection connection) throws IOException {
		instance = this;
		players = new HashSet<>();

		this.connection = connection;
		connection.initialise(input -> {
			Packet packet = input.packet();
			if(packet instanceof GameStatusUpdatePacket gameStatusUpdate) {
				if(!games.containsKey(gameStatusUpdate.gameId())) {
					// A new game has been created, yeey!
					ControllerGameState initialState = new ControllerGameState();
					initialState.applyRecord(gameStatusUpdate.state()); // TODO Not very clean? Maybe in constructor
					games.put(gameStatusUpdate.gameId(),
							new ControllerGame(gameStatusUpdate.gameId(), input.from(),
									gameStatusUpdate.name(), initialState));
					return;
				}
				games.get(gameStatusUpdate.gameId()).update(input.from(), gameStatusUpdate.name(), gameStatusUpdate.state());
			} else if(packet instanceof PlayerJoinGamePacket playerJoinGamePacket) {
				players.add((ControllerPlayer) playerJoinGamePacket.player());
			} else if(packet instanceof PlayerQuitGamePacket playerQuitGamePacket) {
				players.remove((ControllerPlayer) playerQuitGamePacket.player());
			} else if(packet instanceof RequestPartyPacket requestPartyPacket) {
				getPlayer(requestPartyPacket.inviter()).flatMap(ControllerPlayer::getParty).ifPresent(party -> {
					getPlayer(requestPartyPacket.requested().getUuid()).ifPresent(party::sendRequest);
				});
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
		return Optional.ofNullable(games.get(uuid));
	}

	public Set<ControllerPlayer> getPlayers() {
		return players;
	}

	public Collection<ControllerGame> getGames() {
		return games.values();
	}


}
