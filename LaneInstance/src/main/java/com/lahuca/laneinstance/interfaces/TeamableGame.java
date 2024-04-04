package com.lahuca.laneinstance.interfaces;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 04.04.2024
 **/

/**
 * Interface for games with teams
 */
public interface TeamableGame {

    /**
     * Gets all registered Teams
     *
     * @return Set of teams
     */
    Set<GameTeam> getTeams();

    /**
     * Gets a team of given UUID
     *
     * @param uuid The player's uuid
     * @return GameTeam of player, null when he is not in team
     */
    GameTeam getTeamOfPlayer(UUID uuid);

    /**
     * Adds the given Team
     *
     * @param team The team that should be added
     */
    void addTeam(GameTeam team);

    /**
     * Removes the list
     *
     * @param team The list that should be removed
     */
    void removeTeam(GameTeam team);
}
