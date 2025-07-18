package com.lahuca.lane.game;

import com.lahuca.lane.queue.QueueType;

import java.util.HashSet;
import java.util.UUID;

public interface Slottable {

    // TODO Auto clear reserved slots, when it goes invalid after X seconds? Better handling?
    HashSet<UUID> getReserved(); // Reserved slots
    HashSet<UUID> getOnline(); // All online players = reserved minus still connecting.
    HashSet<UUID> getPlayers(); // Only the actual players = online - vanished admins
    HashSet<UUID> getPlaying(); // Only the actual playing players = players - spectators/viewers

    boolean isOnlineJoinable();
    boolean isPlayersJoinable();
    boolean isPlayingJoinable();

    int getMaxOnlineSlots();
    int getMaxPlayersSlots();
    int getMaxPlayingSlots();

    default int getAvailableOnlineSlots() {
        return getMaxOnlineSlots() - getReserved().size();
    }

    default int getAvailablePlayersSlots() {
        return getMaxPlayersSlots() - getPlayers().size();
    }

    default int getAvailablePlayingSlots() {
        return getMaxPlayingSlots() - getPlaying().size();
    }

    default boolean isQueueJoinable(QueueType queueType, int spots) {
        return switch (queueType) {
            case ONLINE -> isOnlineJoinable() && (getMaxOnlineSlots() <= 0 || getAvailableOnlineSlots() >= spots);
            case PLAYERS -> isOnlineJoinable() && (getMaxOnlineSlots() <= 0 || getAvailableOnlineSlots() >= spots)
                    && isPlayersJoinable() && (getMaxPlayersSlots() <= 0 || getAvailablePlayersSlots() >= spots);
            case PLAYING -> isOnlineJoinable() && (getMaxOnlineSlots() <= 0 || getAvailableOnlineSlots() >= spots)
                    && isPlayersJoinable() && (getMaxPlayersSlots() <= 0 || getAvailablePlayersSlots() >= spots)
                    && isPlayingJoinable() && (getMaxPlayingSlots() <= 0 || getAvailablePlayingSlots() >= spots);
        };
    }

    default boolean containsReserved(UUID uuid) {
        return getReserved().contains(uuid);
    }

    default boolean containsOnline(UUID uuid) {
        return getOnline().contains(uuid);
    }

    default boolean containsPlayers(UUID uuid) {
        return getPlayers().contains(uuid);
    }

    default boolean containsPlaying(UUID uuid) {
        return getPlaying().contains(uuid);
    }

}
