package com.lahuca.laneinstancepaper.events;

import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.QueueCancelledEvent;
import org.bukkit.event.HandlerList;

public class PaperQueueCancelledEvent extends PaperInstanceEvent<QueueCancelledEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperQueueCancelledEvent(QueueCancelledEvent instanceEvent) {
        super(instanceEvent);
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public InstancePlayer getPlayer() {
        return getInstanceEvent().getPlayer();
    }

    public QueueRequest getQueue() {
        return getInstanceEvent().getQueue();
    }

    public boolean hasDisconnected() {
        return getInstanceEvent().hasDisconnected();
    }

}
