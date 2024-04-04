package com.lahuca.laneinstance.interfaces;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 04.04.2024
 **/

/**
 *
 */
public interface GameTeam {

    /**
     * Gets the team Id
     *
     * @return The id
     */
    String getId();

    /**
     * Gets the name of the team
     *
     * @return The Team name
     */
    String getName();

    /**
     * Gets all team players
     *
     * @return Set of team players
     */
    Set<UUID> getPlayers();

}
