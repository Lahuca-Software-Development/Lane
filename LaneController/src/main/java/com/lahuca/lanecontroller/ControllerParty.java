package com.lahuca.lanecontroller;

import com.lahuca.lane.LaneParty;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class ControllerParty implements LaneParty {

    private UUID owner;
    private Set<UUID> players;
    private Set<UUID> requested;
    private long creationStamp;

    public ControllerParty(UUID owner, Set<UUID> players, Set<UUID> requested, long creationStamp) {
        this.owner = owner;
        this.players = players;
        this.requested = requested;
        this.creationStamp = creationStamp;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public Set<UUID> getPlayers() {
        return players;
    }

    @Override
    public Set<UUID> getRequested() {
        return requested;
    }

    @Override
    public long getCreationTimestamp() {
        return creationStamp;
    }
}
