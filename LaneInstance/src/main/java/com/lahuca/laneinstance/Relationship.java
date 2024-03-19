package com.lahuca.laneinstance;

import com.lahuca.lane.LaneRelationship;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class Relationship implements LaneRelationship {

    private final Set<UUID> players;

    public Relationship(Set<UUID> players) {
        this.players = players;
    }

    @Override
    public Set<UUID> players() {
        return players;
    }
}
