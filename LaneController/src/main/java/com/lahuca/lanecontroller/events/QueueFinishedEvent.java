package com.lahuca.lanecontroller.events;

import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lanecontroller.ControllerPlayer;

import java.util.Optional;

/**
 * This event is called when a player is done queueing and has successfully joined a game/instance.
 * @param player the player
 * @param queue the total queue request
 * @param instanceId the instance ID of the instance or game the player joined
 * @param gameId the optional game ID
 */
public record QueueFinishedEvent(ControllerPlayer player, QueueRequest queue, String instanceId, Long gameId) implements LanePlayerEvent<ControllerPlayer> {

    @Override
    public ControllerPlayer getPlayer() {
        return player;
    }

    Optional<Long> getGameId() {
        return Optional.ofNullable(gameId);
    }

}
