package com.lahuca.laneinstance.events;

import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.laneinstance.InstancePlayer;

/**
 * This event is called when a player has successfully set a new nickname.
 */
public record NickSetEvent(InstancePlayer player, String newNickname) implements LanePlayerEvent<InstancePlayer> {

    @Override
    public InstancePlayer getPlayer() {
        return player;
    }

}
