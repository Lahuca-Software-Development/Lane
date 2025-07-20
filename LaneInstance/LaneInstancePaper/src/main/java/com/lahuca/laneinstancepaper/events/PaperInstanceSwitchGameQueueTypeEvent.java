package com.lahuca.laneinstancepaper.events;

import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstanceGame;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.InstancePlayerListType;
import com.lahuca.laneinstance.events.InstanceSwitchGameQueueTypeEvent;
import org.bukkit.event.HandlerList;

public class PaperInstanceSwitchGameQueueTypeEvent extends PaperInstanceEvent<InstanceSwitchGameQueueTypeEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperInstanceSwitchGameQueueTypeEvent(InstanceSwitchGameQueueTypeEvent instanceEvent) {
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

    public QueueType newQueueType() {
        return getInstanceEvent().newQueueType();
    }

    public InstanceGame game() {
        return getInstanceEvent().game();
    }

    public InstancePlayerListType oldPlayerListType() {
        return getInstanceEvent().oldPlayerListType();
    }

}
