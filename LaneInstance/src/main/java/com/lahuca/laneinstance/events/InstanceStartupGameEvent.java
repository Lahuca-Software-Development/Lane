package com.lahuca.laneinstance.events;

import com.lahuca.lane.events.LaneGameEvent;
import com.lahuca.laneinstance.game.InstanceGame;

/**
 * This event is called when a game has successfully been registered and created.
 *
 * @param game    the game
 */
public record InstanceStartupGameEvent(InstanceGame game) implements LaneGameEvent<InstanceGame> {

    @Override
    public InstanceGame getGame() {
        return game;
    }

}
