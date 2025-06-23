/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 12:44 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.laneinstance;

import com.lahuca.lane.connection.packet.GameStatusUpdatePacket;
import com.lahuca.lane.game.LaneGame;
import com.lahuca.lane.queue.QueueType;
import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.RecordConverter;
import com.lahuca.lane.records.StatePropertyRecord;

import java.util.*;

public abstract class InstanceGame implements LaneGame, RecordConverter<GameRecord> {

	private final long gameId;
	private final String instanceId;
	private String gameType;
	private String gameMode;
	private String gameMap;

	private final HashSet<UUID> reserved = new HashSet<>();
	private final HashSet<UUID> online = new HashSet<>();
	private final HashSet<UUID> players = new HashSet<>();
	private final HashSet<UUID> playing = new HashSet<>();
	private boolean onlineJoinable;
	private boolean playersJoinable;
	private boolean playingJoinable;
	private int maxOnlineSlots;
	private int maxPlayersSlots;
	private int maxPlayingSlots;

	private String state;
	private final HashMap<String, InstanceStateProperty> properties = new HashMap<>();

	public InstanceGame(long gameId, String instanceId) {
		this.gameId = gameId;
		this.instanceId = instanceId;
	}

	public abstract void onStartup();
	public abstract void onShutdown();
	public abstract void onJoin(InstancePlayer instancePlayer, QueueType queueType);
	public abstract void onQuit(InstancePlayer instancePlayer);

	@Override
	public long getGameId() {
		return gameId;
	}

	@Override
	public String getInstanceId() {
		return instanceId;
	}

	@Override
	public String getGameType() {
		return gameType;
	}

	@Override
	public Optional<String> getGameMode() {
		return Optional.ofNullable(gameMode);
	}

	@Override
	public Optional<String> getGameMap() {
		return Optional.ofNullable(gameMap);
	}

	@Override
	public HashSet<UUID> getReserved() {
		return new HashSet<>(Set.copyOf(reserved));
	}

	void addReserved(UUID uuid) {
		reserved.add(uuid);
		sendGameStatus();
	}

	void removeReserved(UUID uuid) {
		reserved.remove(uuid);
		online.remove(uuid);
		players.remove(uuid);
		playing.remove(uuid);
		sendGameStatus();
	}

	@Override
	public HashSet<UUID> getOnline() {
		return new HashSet<>(Set.copyOf(online));
	}

	public void addOnline(UUID uuid) {
		reserved.add(uuid);
		online.add(uuid);
		sendGameStatus();
	}

	void removeOnline(UUID uuid) {
		online.remove(uuid);
		players.remove(uuid);
		playing.remove(uuid);
		sendGameStatus();
	}

	@Override
	public HashSet<UUID> getPlayers() {
		return new HashSet<>(Set.copyOf(players));
	}

	public void addPlayer(UUID uuid) {
		reserved.add(uuid);
		online.add(uuid);
		players.add(uuid);
		sendGameStatus();
	}

	public void removePlayer(UUID uuid) {
		players.remove(uuid);
		playing.remove(uuid);
		sendGameStatus();
	}

	@Override
	public HashSet<UUID> getPlaying() {
		return new HashSet<>(Set.copyOf(playing));
	}

	public void addPlaying(UUID uuid) {
		reserved.add(uuid);
		online.add(uuid);
		players.add(uuid);
		playing.add(uuid);
		sendGameStatus();
	}

	public void removePlaying(UUID uuid) {
		playing.remove(uuid);
		sendGameStatus();
	}

	@Override
	public boolean isOnlineJoinable() {
		return onlineJoinable;
	}

	@Override
	public boolean isPlayersJoinable() {
		return playersJoinable;
	}

	@Override
	public boolean isPlayingJoinable() {
		return playingJoinable;
	}

	@Override
	public int getMaxOnlineSlots() {
		return maxOnlineSlots;
	}

	@Override
	public int getMaxPlayersSlots() {
		return maxPlayersSlots;
	}

	@Override
	public int getMaxPlayingSlots() {
		return maxPlayingSlots;
	}

	@Override
	public Optional<String> getState() {
		return Optional.ofNullable(state);
	}

	@Override
	public HashMap<String, InstanceStateProperty> getProperties() {
		return properties;
	}

	private void sendGameStatus() {
		LaneInstance.getInstance().getConnection().sendRequestPacket(id -> new GameStatusUpdatePacket(id, convertRecord()), null); // TODO Handle error?
	}

	@Override
	public GameRecord convertRecord() {
		HashMap<String, StatePropertyRecord> propertyRecords = new HashMap<>();
		properties.forEach((k, v) -> propertyRecords.put(k, v.convertRecord()));
		return new GameRecord(gameId, instanceId, gameType, gameMode, gameMap, reserved, online, players, playing,
				onlineJoinable, playersJoinable, playingJoinable, maxOnlineSlots, maxPlayersSlots, maxPlayingSlots,
				state, propertyRecords);
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", InstanceGame.class.getSimpleName() + "[", "]")
				.add("gameId=" + gameId)
				.add("instanceId='" + instanceId + "'")
				.add("gameType='" + gameType + "'")
				.add("gameMode='" + gameMode + "'")
				.add("gameMap='" + gameMap + "'")
				.add("reserved=" + reserved)
				.add("online=" + online)
				.add("players=" + players)
				.add("playing=" + playing)
				.add("onlineJoinable=" + onlineJoinable)
				.add("playersJoinable=" + playersJoinable)
				.add("playingJoinable=" + playingJoinable)
				.add("maxOnlineSlots=" + maxOnlineSlots)
				.add("maxPlayersSlots=" + maxPlayersSlots)
				.add("maxPlayingSlots=" + maxPlayingSlots)
				.add("state='" + state + "'")
				.add("properties=" + properties)
				.toString();
	}

}
