package com.lahuca.laneinstance.game;

import com.lahuca.lane.queue.QueueRequestParameter;
import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.InstancePlayerListType;

import java.util.Optional;
import java.util.Set;

/**
 * @author _Neko1
 * @date 04.04.2024
 **/

/**
 * Interface for games with teams
 */
public interface TeamGame<T extends Team> extends GameLifecycle {

    /**
     * Gets all registered Teams
     *
     * @return Set of teams
     */
    Set<T> getTeams();

    default Optional<T> getTeam(String id) {
        return getTeams().stream().filter(team -> team.getId().equals(id)).findFirst();
    }

    /**
     * Gets a team of given UUID
     *
     * @param uuid The player's uuid
     * @return GameTeam of player, null when he is not in team
     */
    default Optional<T> getTeamOfPlayer(InstancePlayer player) {
        return getTeams().stream().filter(team -> team.isMember(player)).findFirst();
    }

    /**
     * Adds the given Team
     *
     * @param team The team that should be added
     */
    void addTeam(T team);

    /**
     * Removes the list
     *
     * @param team The list that should be removed
     */
    void removeTeam(T team);

    default void onQuit(InstancePlayer instancePlayer) {
        getTeamOfPlayer(instancePlayer).ifPresent(team -> team.removeMember(instancePlayer));
    }

    @Override
    default void onSwitchQueueType(InstancePlayer instancePlayer, InstancePlayerListType oldPlayerListType, QueueType queueType, QueueRequestParameter parameter) {
        if(queueType != QueueType.PLAYING) getTeamOfPlayer(instancePlayer).ifPresent(team -> team.removeMember(instancePlayer));
    }

}
