package com.lahuca.laneinstance.events;

import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.laneinstance.InstancePlayer;

/**
 * This event is called when a player has quit the instance.
 *
 * @param player the player
 */
public record InstanceQuitEvent(InstancePlayer player) implements LanePlayerEvent<InstancePlayer> {

    @Override
    public InstancePlayer getPlayer() {
        return player;
    }

}
