package com.lahuca.laneinstance.events;

import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.InstancePlayerListType;

/**
 * This event is called when a player is queued to the instance it is already present on.
 * This is only called when the queue type of the player changes.
 *
 * @param player            the player
 * @param oldPlayerListType the player's old queue type
 * @param newQueueType      the new queue type
 */
public record InstanceSwitchQueueTypeEvent(InstancePlayer player, InstancePlayerListType oldPlayerListType,
                                           QueueType newQueueType) implements LanePlayerEvent<InstancePlayer> {

    @Override
    public InstancePlayer getPlayer() {
        return player;
    }

}
