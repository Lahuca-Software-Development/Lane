package com.lahuca.lanecontroller;

import com.lahuca.lane.game.LaneGame;
import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.RecordConverterApplier;
import com.lahuca.lane.records.StatePropertyRecord;

import java.util.*;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class ControllerGame implements RecordConverterApplier<GameRecord>, LaneGame {

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
    private boolean onlineKickable;
    private boolean playersKickable;
    private boolean playingKickable;

    private String state;
    private final HashMap<String, ControllerStateProperty> properties = new HashMap<>();

    ControllerGame(GameRecord record) {
        this.gameId = record.gameId();
        this.instanceId = record.instanceId();
        applyRecord(record);
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
    public HashSet<UUID> getOnline() {
        return new HashSet<>(Set.copyOf(online));
    }

    @Override
    public HashSet<UUID> getPlayers() {
        return new HashSet<>(Set.copyOf(players));
    }

    @Override
    public HashSet<UUID> getPlaying() {
        return new HashSet<>(Set.copyOf(playing));
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
    public boolean isOnlineKickable() {
        return onlineKickable;
    }

    @Override
    public boolean isPlayersKickable() {
        return playersKickable;
    }

    @Override
    public boolean isPlayingKickable() {
        return playingKickable;
    }

    @Override
    public Optional<String> getState() {
        return Optional.ofNullable(state);
    }

    @Override
    public HashMap<String, ControllerStateProperty> getProperties() {
        return properties;
    }

    @Override
    public GameRecord convertRecord() {
        HashMap<String, StatePropertyRecord> propertyRecords = new HashMap<>();
        properties.forEach((k, v) -> propertyRecords.put(k, v.convertRecord()));
        return new GameRecord(gameId, instanceId, gameType, gameMode, gameMap, reserved, online, players, playing,
                onlineJoinable, playersJoinable, playingJoinable, maxOnlineSlots, maxPlayersSlots, maxPlayingSlots,
                onlineKickable, playersKickable, playingKickable, state, propertyRecords);
    }

    @Override
    public void applyRecord(GameRecord record) {
        gameType = record.gameType();
        gameMode = record.gameMode();
        gameMap = record.gameMap();
        reserved.clear();
        reserved.addAll(record.reserved());
        online.clear();
        online.addAll(record.online());
        players.clear();
        players.addAll(record.players());
        playing.clear();
        playing.addAll(record.playing());
        onlineJoinable = record.onlineJoinable();
        playersJoinable = record.playersJoinable();
        playingJoinable = record.playingJoinable();
        maxOnlineSlots = record.maxOnlineSlots();
        maxPlayersSlots = record.maxPlayersSlots();
        maxPlayingSlots = record.maxPlayingSlots();
        onlineKickable = record.onlineKickable();
        playersKickable = record.playersKickable();
        playingKickable = record.playingKickable();
        state = record.state();
        properties.clear();
        record.properties().forEach((k, v) -> properties.put(k, new ControllerStateProperty(v.id(), v.value(), v.extraData())));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ControllerGame.class.getSimpleName() + "[", "]")
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
                .add("state='" + state + "'")
                .add("properties=" + properties)
                .toString();
    }

}
