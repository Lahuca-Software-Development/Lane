package com.lahuca.laneinstancepaper.events;

import com.lahuca.laneinstance.events.InstanceEvent;
import org.bukkit.event.HandlerList;

/**
 * This event is called when an instance event is called, but the object type is not known.
 */
public class PaperInstanceGenericEvent extends PaperInstanceEvent<InstanceEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperInstanceGenericEvent(InstanceEvent instanceEvent) {
        super(instanceEvent);
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

}
