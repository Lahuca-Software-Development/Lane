package com.lahuca.laneinstance;

import com.lahuca.lane.LaneRelationship;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public record Relationship(Set<UUID> players) implements LaneRelationship {
}
