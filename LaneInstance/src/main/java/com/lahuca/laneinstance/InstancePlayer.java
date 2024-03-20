package com.lahuca.laneinstance;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LanePlayerState;

import java.util.Optional;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class InstancePlayer implements LanePlayer {

    private final UUID uuid;
    private final String name;
    private String displayName;
    private long gameId;
    private Party party;
    private String language;

    private Relationship relationship;

    public InstancePlayer(UUID uuid, String name, String displayName) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    public void setGameId(long gameId) {
        this.gameId = gameId;
    }

    @Override
    public long getGameId() {
        return gameId;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public LanePlayerState getState() {
        return null;
    }

    @Override
    public Optional<LaneParty> getParty() {
        return Optional.ofNullable(party);
    }
}
