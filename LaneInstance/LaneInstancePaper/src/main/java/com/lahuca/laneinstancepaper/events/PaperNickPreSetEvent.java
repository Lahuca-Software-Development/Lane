package com.lahuca.laneinstancepaper.events;

import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.NickPreSetEvent;
import com.lahuca.laneinstance.events.QueueCancelledEvent;
import org.bukkit.event.HandlerList;

/**
 * This event is called when a player is about to be set a new nickname.
 */
public class PaperNickPreSetEvent extends PaperInstanceEvent<NickPreSetEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperNickPreSetEvent(NickPreSetEvent instanceEvent) {
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

    public String getNewNickname() {
        return getInstanceEvent().getNewNickname();
    }

    public void setNewNickname(String newNickname) {
        getInstanceEvent().setNewNickname(newNickname);
    }

    public boolean isCancelled() {
        return getInstanceEvent().isCancelled();
    }

    public void setCancelled(boolean cancelled) {
        getInstanceEvent().setCancelled(cancelled);
    }

}
