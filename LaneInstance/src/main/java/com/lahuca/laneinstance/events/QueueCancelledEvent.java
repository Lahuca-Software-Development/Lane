package com.lahuca.laneinstance.events;

import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.lane.events.LaneQueueCancelledEvent;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.laneinstance.InstancePlayer;

/**
 * This event is called when a player is done queueing and has not joined a game or instance.
 */
public class QueueCancelledEvent extends LaneQueueCancelledEvent<InstancePlayer> {

    /**
     * Constructs a new Lane event.
     *
     * @param player       the player
     * @param queue        the total queue request
     * @param disconnected whether the player has disconnected the network or if nothing has happened
     */
    public QueueCancelledEvent(InstancePlayer player, QueueRequest queue, boolean disconnected) {
        super(player, queue, disconnected);
    }

}
