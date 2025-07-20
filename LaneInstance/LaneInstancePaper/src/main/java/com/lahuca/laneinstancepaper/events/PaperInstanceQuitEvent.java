package com.lahuca.laneinstancepaper.events;

import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.InstanceQuitEvent;
import org.bukkit.event.HandlerList;

public class PaperInstanceQuitEvent extends PaperInstanceEvent<InstanceQuitEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperInstanceQuitEvent(InstanceQuitEvent instanceEvent) {
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

}
