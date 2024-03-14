package com.lahuca.lanegame;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.LanePlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class GamePlayer implements LanePlayer {

    private UUID uuid;
    private String name;
    private String displayName;
    private Party party;

    public GamePlayer(UUID uuid, String name, String displayName) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
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
    public Optional<LaneParty> getParty() {
        return Optional.ofNullable(party);
    }

}
