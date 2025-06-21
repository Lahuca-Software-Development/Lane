package com.lahuca.laneinstance;

import com.lahuca.lane.LaneParty;

import java.util.HashSet;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/

public class Party implements LaneParty {

    private final long partyId;
    private UUID owner;
    private final HashSet<UUID> players;
    private final long creationStamp;

    public Party(long partyId, UUID owner, HashSet<UUID> players, long creationStamp) {
        this.partyId = partyId;
        this.owner = owner;
        this.players = players;
        this.creationStamp = creationStamp;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    @Override
    public long getId() {
        return partyId;
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
    public HashSet<UUID> getPlayers() {
        return players;
    }
}
