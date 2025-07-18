package com.lahuca.laneinstance.events;

import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstanceGame;
import com.lahuca.laneinstance.InstancePlayer;

/**
 * This event is called when a player has successfully joined a game and can be handled.
 *
 * @param player    the player
 * @param game      the game
 * @param queueType the queue type
 */
public record InstanceJoinGameEvent(InstancePlayer player, InstanceGame game,
                                    QueueType queueType) implements InstancePlayerEvent, InstanceGameEvent {

    @Override
    public InstancePlayer getPlayer() {
        return player;
    }

    @Override
    public InstanceGame getGame() {
        return game;
    }

}
