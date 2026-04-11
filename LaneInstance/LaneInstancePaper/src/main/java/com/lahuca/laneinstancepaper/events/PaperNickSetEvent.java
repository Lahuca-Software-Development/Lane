package com.lahuca.laneinstancepaper.events;

import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.NickPreSetEvent;
import com.lahuca.laneinstance.events.NickSetEvent;
import org.bukkit.event.HandlerList;

/**
 * This event is called when a player has successfully set a new nickname.
 */
public class PaperNickSetEvent extends PaperInstanceEvent<NickSetEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperNickSetEvent(NickSetEvent instanceEvent) {
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

    public String newNickname() {
        return getInstanceEvent().newNickname();
    }

}
