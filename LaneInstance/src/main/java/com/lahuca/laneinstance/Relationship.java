package com.lahuca.laneinstance;

import com.lahuca.lane.LaneRelationship;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/

public record Relationship(long relationshipId, Set<UUID> players) implements LaneRelationship {

    @Override
    public long getId() {
        return relationshipId;
    }

    @Override
    public Set<UUID> getPlayers() {
        return players;
    }
}
