package com.lahuca.laneinstance.interfaces;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 02.04.2024
 **/

/**
 * For games which are spectateable
 */
public interface SpectateableGame  {

    /**
     * Gets the all spectators
     *
     * @return The Set of spectating player uuids
     */
    Set<UUID> getSpectators();

    /**
     * What should happen when player joins spectator
     *
     * @param spectator The uuid of spectator
     */
    void joinSpectator(UUID spectator);

    /**
     * What should happen when player leaves spectator
     *
     * @param spectator The uuid of spectator
     */
    void leaveSpectator(UUID spectator);

    /**
     * Returns if player is spectator
     *
     * @param uuid The uuid of player
     * @return true if player is spectator otherwise false
     */
    default boolean isSpectator(UUID uuid) {
        return getSpectators().contains(uuid);
    }
}
