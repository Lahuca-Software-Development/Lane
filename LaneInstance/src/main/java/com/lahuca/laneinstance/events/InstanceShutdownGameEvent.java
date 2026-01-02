package com.lahuca.laneinstance.events;

import com.lahuca.lane.events.LaneGameEvent;
import com.lahuca.laneinstance.game.InstanceGame;

/**
 * This event is called when a game is about to be shutdown.
 * The game is still registered. After running this event, it will be unregistered.
 *
 * @param game    the game
 */
public record InstanceShutdownGameEvent(InstanceGame game) implements LaneGameEvent<InstanceGame> {

    @Override
    public InstanceGame getGame() {
        return game;
    }

}
