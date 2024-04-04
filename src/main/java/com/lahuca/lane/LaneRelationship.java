package com.lahuca.lane;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public interface LaneRelationship {

    long getId();
    Set<UUID> getPlayers();

}