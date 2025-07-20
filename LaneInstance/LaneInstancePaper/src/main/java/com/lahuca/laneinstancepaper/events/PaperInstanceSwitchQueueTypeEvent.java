package com.lahuca.laneinstancepaper.events;

import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.InstancePlayerListType;
import com.lahuca.laneinstance.events.InstanceSwitchQueueTypeEvent;
import org.bukkit.event.HandlerList;

public class PaperInstanceSwitchQueueTypeEvent extends PaperInstanceEvent<InstanceSwitchQueueTypeEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperInstanceSwitchQueueTypeEvent(InstanceSwitchQueueTypeEvent instanceEvent) {
        super(instanceEvent);
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public InstancePlayer player() {
        return getInstanceEvent().player();
    }

    public InstancePlayer getPlayer() {
        return getInstanceEvent().getPlayer();
    }

    public InstancePlayerListType oldPlayerListType() {
        return getInstanceEvent().oldPlayerListType();
    }

    public QueueType newQueueType() {
        return getInstanceEvent().newQueueType();
    }
}
