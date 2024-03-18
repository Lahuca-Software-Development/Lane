package com.lahuca.lanegame;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LaneRelationship;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class Relationship implements LaneRelationship {

    private Set<LanePlayer> players;
    private Set<UUID> requested;

    public Relationship(Set<LanePlayer> players, Set<UUID> requested) {
        this.players = players;
        this.requested = requested;
    }


    @Override
    public Set<LanePlayer> getPlayers() {
        return players;
    }

    @Override
    public Set<UUID> getRequested() {
        return requested;
    }
}
