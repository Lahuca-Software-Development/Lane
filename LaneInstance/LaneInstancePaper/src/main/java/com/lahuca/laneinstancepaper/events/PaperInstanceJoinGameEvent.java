package com.lahuca.laneinstancepaper.events;

import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.InstanceJoinGameEvent;
import com.lahuca.laneinstance.game.InstanceGame;
import org.bukkit.event.HandlerList;

public class PaperInstanceJoinGameEvent extends PaperInstanceEvent<InstanceJoinGameEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperInstanceJoinGameEvent(InstanceJoinGameEvent instanceEvent) {
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

    public QueueType queueType() {
        return getInstanceEvent().queueType();
    }

}
