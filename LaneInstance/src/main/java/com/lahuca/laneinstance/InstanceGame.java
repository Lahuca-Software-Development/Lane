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
	private boolean onlineJoinable = true;
	private boolean playersJoinable = true;
	private boolean playingJoinable = true;
	private int maxOnlineSlots = -1;
	private int maxPlayersSlots = -1;
	private int maxPlayingSlots = -1;
    private boolean onlineKickable = false;
    private boolean playersKickable = false;
    private boolean playingKickable = false;
    private boolean isPrivate = false;

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
	public abstract void onSwitchQueueType(InstancePlayer instancePlayer, InstancePlayerListType oldPlayerListType, QueueType queueType);

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

	@Override
	public boolean containsReserved(UUID uuid) {
		return reserved.contains(uuid);
	}

	@Override
	public boolean containsOnline(UUID uuid) {
		return online.contains(uuid);
	}

	@Override
	public boolean containsPlayers(UUID uuid) {
		return players.contains(uuid);
	}

	@Override
	public boolean containsPlaying(UUID uuid) {
		return playing.contains(uuid);
	}

	/**
	 * Add the player to the lists of the given queue type.
	 * Also removes them from the lists that it does not belong to.
	 * @param uuid the player
	 * @param queueType the queue type
	 */
	protected void applyQueueType(UUID uuid, QueueType queueType) {
		Objects.requireNonNull(uuid);
		Objects.requireNonNull(queueType);
		switch (queueType) {
			case ONLINE -> {
				online.add(uuid);
				players.remove(uuid);
				playing.remove(uuid);
			}
			case PLAYERS -> {
				online.add(uuid);
				players.add(uuid);
				playing.remove(uuid);
			}
			case PLAYING -> {
				online.add(uuid);
				players.add(uuid);
				playing.add(uuid);
			}
		}
		sendGameStatus();
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

	/**
	 * Retrieves the player list type of the player with the given UUID of this game.
	 * @param player the player
	 * @return the player list type, {@link InstancePlayerListType#NONE} if not in a list
	 */
	public InstancePlayerListType getGamePlayerListType(UUID player) {
		if(playing.contains(player)) return InstancePlayerListType.PLAYING;
		if(players.contains(player)) return InstancePlayerListType.PLAYERS;
		if(online.contains(player)) return InstancePlayerListType.ONLINE;
		if(reserved.contains(player)) return InstancePlayerListType.RESERVED;
		return InstancePlayerListType.NONE;
	}

	@Override
	public boolean isOnlineJoinable() {
		return onlineJoinable;
	}

	public void setOnlineJoinable(boolean onlineJoinable) {
		this.onlineJoinable = onlineJoinable;
		sendGameStatus();
	}

	@Override
	public boolean isPlayersJoinable() {
		return playersJoinable;
	}

	public void setPlayersJoinable(boolean playersJoinable) {
		this.playersJoinable = playersJoinable;
		sendGameStatus();
	}

	@Override
	public boolean isPlayingJoinable() {
		return playingJoinable;
	}

	public void setPlayingJoinable(boolean playingJoinable) {
		this.playingJoinable = playingJoinable;
		sendGameStatus();
	}

	@Override
	public int getMaxOnlineSlots() {
		return maxOnlineSlots;
	}

	public void setMaxOnlineSlots(int maxOnlineSlots) {
		this.maxOnlineSlots = maxOnlineSlots;
		sendGameStatus();
	}

	@Override
	public int getMaxPlayersSlots() {
		return maxPlayersSlots;
	}

	public void setMaxPlayersSlots(int maxPlayersSlots) {
		this.maxPlayersSlots = maxPlayersSlots;
		sendGameStatus();
	}

	@Override
	public int getMaxPlayingSlots() {
		return maxPlayingSlots;
	}

	public void setMaxPlayingSlots(int maxPlayingSlots) {
		this.maxPlayingSlots = maxPlayingSlots;
		sendGameStatus();
	}

    @Override
    public boolean isOnlineKickable() {
        return onlineKickable;
    }

    public void setOnlineKickable(boolean onlineKickable) {
        this.onlineKickable = onlineKickable;
        sendGameStatus();
    }

    @Override
    public boolean isPlayersKickable() {
        return playersKickable;
    }

    public void setPlayersKickable(boolean playersKickable) {
        this.playersKickable = playersKickable;
        sendGameStatus();
    }

    @Override
    public boolean isPlayingKickable() {
        return playingKickable;
    }

    public void setPlayingKickable(boolean playingKickable) {
        this.playingKickable = playingKickable;
        sendGameStatus();
    }

    @Override
    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
        sendGameStatus();
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
                onlineKickable, playersKickable, playingKickable, isPrivate,
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
                .add("onlineKickable=" + onlineKickable)
                .add("playersKickable=" + playersKickable)
                .add("playingKickable=" + playingKickable)
                .add("isPrivate=" + isPrivate)
				.add("state='" + state + "'")
				.add("properties=" + properties)
				.toString();
	}

}
