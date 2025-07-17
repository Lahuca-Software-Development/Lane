package com.lahuca.lanecontroller.events;

import com.lahuca.lanecontroller.ControllerPlayer;
import net.kyori.adventure.text.Component;

/**
 * This event is called when a player has joined the network.
 * This lets plugins set whether they still need to do additional processing.
 * There is a maximum timeframe until a plugin needs to be done.
 * A plugin can mark that is it done with processing using {@link ControllerPlayer#process(boolean, Component)}.
 */
public class PlayerNetworkProcessEvent implements ControllerPlayerEvent {

    private final ControllerPlayer player;
    private boolean needProcessing = false;

    public PlayerNetworkProcessEvent(ControllerPlayer player) {
        this.player = player;
    }

    @Override
    public ControllerPlayer getPlayer() {
        return player;
    }

    /**
     * Returns whether a plugin has marked that it needs to do additional processing.
     * @return if additional processing still needs to be done.
     */
    public boolean needsProcessing() {
        return needProcessing;
    }

    /**
     * Marks that a plugin still needs to do additional processing.
     */
    public void setNeedProcessing() {
        needProcessing = true;
    }

}
