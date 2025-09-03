package com.lahuca.laneinstance.game;

import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.StatePropertyRecord;
import com.lahuca.laneinstance.InstanceStateProperty;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractInstanceGame implements InstanceGame {

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

    public AbstractInstanceGame(long gameId, String instanceId) {
        this.gameId = gameId;
        this.instanceId = instanceId;
    }

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
    public void setGameType(@NotNull String gameType) {
        this.gameType = gameType;
        sendGameStatus();
    }

    @Override
    public Optional<String> getGameMode() {
        return Optional.ofNullable(gameMode);
    }

    @Override
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
        sendGameStatus();
    }

    @Override
    public Optional<String> getGameMap() {
        return Optional.ofNullable(gameMap);
    }

    @Override
    public void setGameMap(String gameMap) {
        this.gameMap = gameMap;
        sendGameStatus();
    }

    @Override
    public HashSet<UUID> getReserved() {
        return reserved;
    }

    @Override
    public HashSet<UUID> getOnline() {
        return online;
    }

    @Override
    public HashSet<UUID> getPlayers() {
        return players;
    }

    @Override
    public HashSet<UUID> getPlaying() {
        return playing;
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

    public void setState(String state) {
        this.state = state;
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

    @Override
    public GameRecord convertRecord() {
        HashMap<String, StatePropertyRecord> propertyRecords = new HashMap<>();
        properties.forEach((k, v) -> propertyRecords.put(k, v.convertRecord()));
        return new GameRecord(gameId, instanceId, gameType, gameMode, gameMap, reserved, online, players, playing,
                onlineJoinable, playersJoinable, playingJoinable, maxOnlineSlots, maxPlayersSlots, maxPlayingSlots,
                onlineKickable, playersKickable, playingKickable, isPrivate,
                state, propertyRecords);
    }

}
