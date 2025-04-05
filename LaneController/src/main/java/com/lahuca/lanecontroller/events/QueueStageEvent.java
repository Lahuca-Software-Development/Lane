/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 29-7-2024 at 22:57 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller.events;

import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lanecontroller.ControllerPlayer;
import net.kyori.adventure.text.Component;

import java.util.Set;
import java.util.UUID;

/**
 * This event is called when a queue stage is taking place.
 * Either when a queue request has been called, then there are no {@link com.lahuca.lane.queue.QueueStage} objects in the request.
 * Or when a previous {@link com.lahuca.lane.queue.QueueStage} has failed.
 * The event also retrieves the type of action to do next.
 */
public class QueueStageEvent implements ControllerPlayerEvent {

    private final ControllerPlayer player;
    private final QueueRequest queueRequest;
    private QueueStageEventResult result;

    public QueueStageEvent(ControllerPlayer player, QueueRequest queueRequest) {
        this.player = player;
        this.queueRequest = queueRequest;
        setNoneResult();
    }

    @Override
    public ControllerPlayer getPlayer() {
        return player;
    }

    /**
     * Whether this is the first time the {@link QueueStageEvent} is being called for this {@link QueueRequest}.
     * Meaning there are no {@link com.lahuca.lane.queue.QueueStage} yet in the {@link QueueRequest}, a first action is wanted.
     * @return If this is the initial request.
     */
    public boolean isInitialRequest() {
        return queueRequest == null || queueRequest.getFirstStage().isEmpty();
    }

    public QueueRequest getQueueRequest() {
        return queueRequest;
    }

    public QueueStageEventResult getResult() {
        return result;
    }


    public void setResult(QueueStageEventResult result) {
        this.result = result;
    }

    public void setNoneResult() {
        result = new QueueStageEventResult.None();
    }

    public void setNoneResult(Component message) {
        result = new QueueStageEventResult.None(message);
    }

    public void setDisconnectResult() {
        result = new QueueStageEventResult.Disconnect();
    }

    public void setDisconnectResult(Component message) {
        result = new QueueStageEventResult.Disconnect(message);
    }

    public void setJoinInstanceResult(String instanceId) {
        result = new QueueStageEventResult.JoinInstance(instanceId);
    }

    public void setJoinInstanceResult(String instanceId, Set<UUID> joinTogetherPlayers) {
        result = new QueueStageEventResult.JoinInstance(instanceId, joinTogetherPlayers);
    }

    public void setJoinGameResult(long gameId) {
        result = new QueueStageEventResult.JoinGame(gameId);
    }

    public void setJoinGameResult(long gameId, Set<UUID> joinTogetherPlayers) {
        result = new QueueStageEventResult.JoinGame(gameId, joinTogetherPlayers);
    }

}
