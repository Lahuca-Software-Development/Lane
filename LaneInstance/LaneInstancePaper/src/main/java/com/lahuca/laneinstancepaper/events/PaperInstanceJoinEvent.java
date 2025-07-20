package com.lahuca.laneinstancepaper.events;

import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.InstanceJoinEvent;
import org.bukkit.event.HandlerList;

public class PaperInstanceJoinEvent extends PaperInstanceEvent<InstanceJoinEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperInstanceJoinEvent(InstanceJoinEvent instanceEvent) {
        super(instanceEvent);
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public QueueType queueType() {
        return getInstanceEvent().queueType();
    }

    public InstancePlayer player() {
        return getInstanceEvent().player();
    }

    public InstancePlayer getPlayer() {
        return getInstanceEvent().getPlayer();
    }
}
