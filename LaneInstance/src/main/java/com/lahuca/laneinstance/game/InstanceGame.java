package com.lahuca.laneinstance.game;

import com.lahuca.lane.connection.packet.GameStatusUpdatePacket;
import com.lahuca.lane.game.LaneGame;
import com.lahuca.lane.queue.QueueType;
import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.RecordConverter;
import com.lahuca.laneinstance.InstancePlayerListType;
import com.lahuca.laneinstance.LaneInstance;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public interface InstanceGame extends GameLifecycle, LaneGame, RecordConverter<GameRecord> {

    void setGameType(@NotNull String type);
    void setGameMode(String mode);
    void setGameMap(String map);

    // TODO Add javadocs to the methods below to NEVER use them outside of the Instance itself.
    default void addReserved(UUID uuid) {
        getReserved().add(uuid);
        sendGameStatus();
    }
    default void removeReserved(UUID uuid) {
        getReserved().remove(uuid);
        getOnline().remove(uuid);
        getPlayers().remove(uuid);
        getPlaying().remove(uuid);
        sendGameStatus();
    }
    default void addOnline(UUID uuid) {
        getReserved().add(uuid);
        getOnline().add(uuid);
        sendGameStatus();
    }
    default void removeOnline(UUID uuid) {
        getOnline().remove(uuid);
        getPlayers().remove(uuid);
        getPlaying().remove(uuid);
        sendGameStatus();
    }
    default void addPlayer(UUID uuid) {
        getReserved().add(uuid);
        getOnline().add(uuid);
        getPlayers().add(uuid);
        sendGameStatus();
    }
    default void removePlayer(UUID uuid) {
        getPlayers().remove(uuid);
        getPlaying().remove(uuid);
        sendGameStatus();
    }

    /**
     * Add the player to the lists of the given queue type.
     * Also removes them from the lists that it does not belong to.
     * @param uuid the player
     * @param queueType the queue type
     */
    default void applyQueueType(UUID uuid, QueueType queueType) {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(queueType);
        switch (queueType) {
            case ONLINE -> {
                getOnline().add(uuid);
                getPlayers().remove(uuid);
                getPlaying().remove(uuid);
            }
            case PLAYERS -> {
                getOnline().add(uuid);
                getPlayers().add(uuid);
                getPlaying().remove(uuid);
            }
            case PLAYING -> {
                getOnline().add(uuid);
                getPlayers().add(uuid);
                getPlaying().add(uuid);
            }
        }
        sendGameStatus();
    }

    /**
     * Retrieves the player list type of the player with the given UUID of this game.
     * @param player the player
     * @return the player list type, {@link InstancePlayerListType#NONE} if not in a list
     */
    default InstancePlayerListType getGamePlayerListType(UUID player) {
        if(containsPlaying(player)) return InstancePlayerListType.PLAYING;
        if(containsPlayers(player)) return InstancePlayerListType.PLAYERS;
        if(containsOnline(player)) return InstancePlayerListType.ONLINE;
        if(containsReserved(player)) return InstancePlayerListType.RESERVED;
        return InstancePlayerListType.NONE;
    }

    void setOnlineJoinable(boolean onlineJoinable);
    void setPlayersJoinable(boolean playersJoinable);
    void setPlayingJoinable(boolean playingJoinable);
    void setMaxOnlineSlots(int maxOnlineSlots);
    void setMaxPlayersSlots(int maxPlayersSlots);
    void setMaxPlayingSlots(int maxPlayingSlots);
    void setOnlineKickable(boolean onlineKickable);
    void setPlayersKickable(boolean playersKickable);
    void setPlayingKickable(boolean playingKickable);

    void setPrivate(boolean isPrivate);

    /**
     * Send the current game data to the Controller.
     */
    default void sendGameStatus() {
        LaneInstance.getInstance().getConnection().sendRequestPacket(id -> new GameStatusUpdatePacket(id, convertRecord()), null); // TODO Handle error?
    }

}
