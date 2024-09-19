/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 30-7-2024 at 21:27 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller.events;

import com.lahuca.lane.queue.QueueStage;
import com.lahuca.lane.queue.QueueStageResult;

import java.util.Set;
import java.util.UUID;

/**
 * Defines the base class for the result for the queue stage failed event.
 */
public sealed class QueueStageEventResult permits QueueStageEventResult.None, QueueStageEventResult.Disconnect,
        QueueStageEventResult.QueueStageEventStageableResult {

    /**
     * Result that tells the queue system that this is not the final stage of the event.
     */
    public sealed static abstract class QueueStageEventStageableResult extends QueueStageEventResult
            permits JoinInstance, JoinGame {

        public abstract QueueStage constructStage(QueueStageResult reason);

    }

    /**
     * The class consisting of the data for doing nothing with the event.
     * This should be set when the player should stay at its server and the queue should be closed.
     * An additional message can be provided to be sent to the player is possible, useful when trying to join a different instance/game which fails.
     */
    public final static class None extends QueueStageEventResult {

        private final String message;

        public None() {
            message = null;
        }

        public None(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

    }

    /**
     * The class consisting of the data for a disconnect result.
     * The queue request will be closed.
     */
    public final static class Disconnect extends QueueStageEventResult {

        private final String message;

        public Disconnect(String message) {
            this.message = message;
        }

        public Disconnect() {
            message = null;
        }

        public String getMessage() {
            return message;
        }

    }

    /**
     * The class consisting of the data for an instance join result.
     * It also holds any other players (in UUID form) that should also do the same interaction.
     */
    public final static class JoinInstance extends QueueStageEventStageableResult {

        private final String instanceId;
        private final Set<UUID> joinTogetherPlayers;

        public JoinInstance(String instanceId) {
            this(instanceId, null);
        }

        public JoinInstance(String instanceId, Set<UUID> joinTogetherPlayers) {
            this.instanceId = instanceId;
            this.joinTogetherPlayers = joinTogetherPlayers;
        }

        public String getInstanceId() {
            return instanceId;
        }

        /**
         * The players (in UUID form) that should also try to join the given instance ID.
         * @return A set of UUIDs of the players that should also join the given instance ID.
         */
        public Set<UUID> getJoinTogetherPlayers() {
            return joinTogetherPlayers;
        }

        @Override
        public QueueStage constructStage(QueueStageResult reason) {
            return new QueueStage(reason, instanceId, null);
        }
    }

    /**
     * The class consisting of the data for a game join result.
     * It also holds any other players (in UUID form) that should also do the same interaction.
     */
    public final static class JoinGame extends QueueStageEventStageableResult {

        private final long gameId;
        private final Set<UUID> joinTogetherPlayers;

        public JoinGame(long gameId) {
            this(gameId, null);
        }

        public JoinGame(long gameId, Set<UUID> joinTogetherPlayers) {
            this.gameId = gameId;
            this.joinTogetherPlayers = joinTogetherPlayers;
        }

        public long getGameId() {
            return gameId;
        }

        /**
         * The players (in UUID form) that should also try to join the given instance ID.
         * @return A set of UUIDs of the players that should also join the given instance ID.
         */
        public Set<UUID> getJoinTogetherPlayers() {
            return joinTogetherPlayers;
        }

        @Override
        public QueueStage constructStage(QueueStageResult reason) {
            return new QueueStage(reason, null, gameId);
        }
    }

}
