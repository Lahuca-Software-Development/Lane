package com.lahuca.laneinstance.events;

import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstancePlayer;

/**
 * This event is called when a player has successfully joined the instance and can be handled.
 *
 * @param player    the player
 * @param queueType the queue type
 */
public record InstanceJoinEvent(InstancePlayer player, QueueType queueType) implements LanePlayerEvent<InstancePlayer> {

    @Override
    public InstancePlayer getPlayer() {
        return player;
    }

}
