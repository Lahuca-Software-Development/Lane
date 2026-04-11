package com.lahuca.laneinstance.events;

import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.laneinstance.InstancePlayer;

/**
 * This event is called when a player is about to be set a new nickname.
 */
public class NickPreSetEvent implements LanePlayerEvent<InstancePlayer> {

    private final InstancePlayer player;
    private String newNickname;
    private boolean cancelled = false;

    public NickPreSetEvent(InstancePlayer player, String newNickname) {
        this.player = player;
        this.newNickname = newNickname;
    }

    @Override
    public InstancePlayer getPlayer() {
        return player;
    }

    public String getNewNickname() {
        return newNickname;
    }

    public void setNewNickname(String newNickname) {
        this.newNickname = newNickname;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
