package com.lahuca.laneinstance;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LaneRelationship;

import java.util.Set;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class Relationship implements LaneRelationship {

    private Set<LanePlayer> players;

    public Relationship(Set<LanePlayer> players) {
        this.players = players;
    }


    @Override
    public Set<LanePlayer> getPlayers() {
        return players;
    }
}
