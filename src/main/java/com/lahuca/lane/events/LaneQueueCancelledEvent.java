package com.lahuca.lane.events;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.queue.QueueRequest;

/**
 * This event is called when a player is done queueing and has not joined a game or instance.
 *
 */
public abstract class LaneQueueCancelledEvent<P extends LanePlayer> implements LanePlayerEvent<P> {

    private final P player;
    private final QueueRequest queue;
    private final boolean disconnected;

    /**
     * Constructs a new Lane event.
     *
     * @param player       the player
     * @param queue        the total queue request
     * @param disconnected whether the player has disconnected the network or if nothing has happened
     */
    public LaneQueueCancelledEvent(P player, QueueRequest queue, boolean disconnected) {
        this.player = player;
        this.queue = queue;
        this.disconnected = disconnected;
    }

    @Override
    public P getPlayer() {
        return player;
    }

    public QueueRequest getQueue() {
        return queue;
    }

    public boolean hasDisconnected() {
        return disconnected;
    }

}
