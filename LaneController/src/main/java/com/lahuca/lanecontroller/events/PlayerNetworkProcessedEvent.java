package com.lahuca.lanecontroller.events;

import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.lanecontroller.ControllerPlayer;

/**
 * This event is called when a player has joined the network and all plugins have processed the player.
 * @param player the player
 */
public record PlayerNetworkProcessedEvent(ControllerPlayer player) implements LanePlayerEvent<ControllerPlayer> {

    @Override
    public ControllerPlayer getPlayer() {
        return player;
    }

}
