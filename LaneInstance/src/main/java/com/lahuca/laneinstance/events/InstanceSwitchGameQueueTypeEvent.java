package com.lahuca.laneinstance.events;

import com.lahuca.lane.events.LaneGameEvent;
import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.InstancePlayerListType;
import com.lahuca.laneinstance.game.InstanceGame;

/**
 * This event is called when a player is queued to the game it is already present on.
 * This is only called when the queue type of the player changes.
 *
 * @param player            the player
 * @param game              the game
 * @param oldPlayerListType the player's old queue type
 * @param newQueueType      the new queue type
 */
public record InstanceSwitchGameQueueTypeEvent(InstancePlayer player, InstanceGame game,
                                               InstancePlayerListType oldPlayerListType,
                                               QueueType newQueueType) implements LanePlayerEvent<InstancePlayer>, LaneGameEvent<InstanceGame> {

    @Override
    public InstancePlayer getPlayer() {
        return player;
    }

    @Override
    public InstanceGame getGame() {
        return game;
    }

}
