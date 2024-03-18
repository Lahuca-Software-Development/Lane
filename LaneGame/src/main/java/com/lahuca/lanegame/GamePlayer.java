package com.lahuca.lanegame;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LanePlayerState;

import java.util.Optional;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class GamePlayer implements LanePlayer {

    private final UUID uuid;
    private final String name;
    private String displayName;
    private Party party;
    private String language;

    public GamePlayer(UUID uuid, String name, String displayName) {
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
