package com.lahuca.laneinstance.events;

import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.game.InstanceGame;

/**
 * This event is called when a player has quit a game.
 * This does not have to mean it also quit the instance.
 *
 * @param player    the player
 * @param game      the game
 */
public record InstanceQuitGameEvent(InstancePlayer player, InstanceGame game) implements InstancePlayerEvent, InstanceGameEvent {

    @Override
    public InstancePlayer getPlayer() {
        return player;
    }

    @Override
    public InstanceGame getGame() {
        return game;
    }

}
