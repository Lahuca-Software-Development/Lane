package com.lahuca.laneinstancepaper.events;

import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.InstanceQuitGameEvent;
import com.lahuca.laneinstance.game.InstanceGame;
import org.bukkit.event.HandlerList;

public class PaperInstanceQuitGameEvent extends PaperInstanceEvent<InstanceQuitGameEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperInstanceQuitGameEvent(InstanceQuitGameEvent instanceEvent) {
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

    public InstanceGame getGame() {
        return getInstanceEvent().getGame();
    }

    public InstanceGame game() {
        return getInstanceEvent().game();
    }

}
