package com.lahuca.laneinstancepaper.events;

import com.lahuca.laneinstance.InstanceGame;
import com.lahuca.laneinstance.events.InstanceShutdownGameEvent;
import org.bukkit.event.HandlerList;

public class PaperInstanceShutdownGameEvent extends PaperInstanceEvent<InstanceShutdownGameEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperInstanceShutdownGameEvent(InstanceShutdownGameEvent instanceEvent) {
        super(instanceEvent);
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public InstanceGame game() {
        return getInstanceEvent().game();
    }

    public InstanceGame getGame() {
        return getInstanceEvent().getGame();
    }

}
