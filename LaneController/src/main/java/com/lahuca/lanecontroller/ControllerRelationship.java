package com.lahuca.lanecontroller;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LaneRelationship;
import com.lahuca.lane.records.RelationshipRecord;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/

public record ControllerRelationship(Set<LanePlayer> players, Set<UUID> requested) implements LaneRelationship {
    public RelationshipRecord convertToRecord() {
        return new RelationshipRecord(players);
    }

    public void removePlayer(ControllerPlayer controllerPlayer) {
        players.remove(controllerPlayer);
    }
}
