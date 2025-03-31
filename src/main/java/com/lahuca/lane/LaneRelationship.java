package com.lahuca.lane;

import java.util.Set;
import java.util.UUID;

/**
 * Base object for relationships between players.
 * This is used for parties and friends.
 * These can be implemented for other features: guilds, etc.
 *
 * @author _Neko1
 * @author Laurenshup
 * @date 14.03.2024
 **/
public interface LaneRelationship {

    long getId();
    Set<UUID> getPlayers();

}