package com.lahuca.laneinstance;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.LanePlayer;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/

public class Party implements LaneParty {

    private UUID owner;
    private Set<LanePlayer> players;
    private final long creationStamp;

    public Party(UUID owner, Set<LanePlayer> players, long creationStamp) {
        this.owner = owner;
        this.players = players;
        this.creationStamp = creationStamp;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public long getCreationTimestamp() {
        return creationStamp;
    }

    @Override
    public Set<LanePlayer> players() {
        return players;
    }
}
