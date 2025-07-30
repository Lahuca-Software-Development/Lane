package com.lahuca.laneinstance.events;

import com.lahuca.laneinstance.InstanceGame;

/**
 * This event is called when a game has successfully been registered and created.
 *
 * @param game    the game
 */
public record InstanceStartupGameEvent(InstanceGame game) implements InstanceGameEvent {

    @Override
    public InstanceGame getGame() {
        return game;
    }

}
