package com.lahuca.laneinstance.game;

import com.lahuca.laneinstance.InstancePlayer;
import net.kyori.adventure.text.ComponentLike;

import java.util.Set;

/**
 * @author _Neko1
 * @date 04.04.2024
 **/
public interface Team extends ComponentLike {

    // TODO Actually also add options for TeamColor, friendly fire, and other MC stuff.
    //  This is connected with a ScoreboardManager

    /**
     * Gets the team Id
     *
     * @return The id
     */
    String getId();

    /**
     * Gets all team players
     *
     * @return Set of team players
     */
    Set<InstancePlayer> getMembers();

    default boolean isMember(InstancePlayer player) {
        return getMembers().contains(player);
    }

    default void addMember(InstancePlayer player) {
        getMembers().add(player);
    }

    default void removeMember(InstancePlayer player) {
        getMembers().remove(player);
    }

}
