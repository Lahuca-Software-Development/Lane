package com.lahuca.lanecontroller;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LaneRelationship;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/

public class ControllerRelationship implements LaneRelationship {

    private final Set<LanePlayer> players;
    private final Set<UUID> requested;

    public ControllerRelationship(Set<LanePlayer> players, Set<UUID> requested) {
        this.players = players;
        this.requested = requested;
    }

    public Set<UUID> getRequested() {
        return requested;
    }

    @Override
    public Set<LanePlayer> getPlayers() {
        return players;
    }
}
