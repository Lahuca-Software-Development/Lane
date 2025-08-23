/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 23-3-2024 at 12:16 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller;

import com.lahuca.lane.game.Slottable;
import com.lahuca.lane.records.InstanceRecord;
import com.lahuca.lane.records.RecordConverterApplier;

import java.util.*;

public final class ControllerLaneInstance implements RecordConverterApplier<InstanceRecord>, Slottable {

    private final String id;
    private String type;

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


    ControllerLaneInstance(InstanceRecord record) {
        this.id = record.id();
        applyRecord(record);
    }

    public String getId() {
        return id;
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
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
    public InstanceRecord convertRecord() {
        return new InstanceRecord(id, type, reserved, online, players, playing, onlineJoinable, playersJoinable,
                playingJoinable, maxOnlineSlots, maxPlayersSlots, maxPlayingSlots, onlineKickable, playersKickable, playingKickable);
    }

    @Override
    public void applyRecord(InstanceRecord record) {
        type = record.type();
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
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ControllerLaneInstance.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("type='" + type + "'")
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
                .toString();
    }

}
