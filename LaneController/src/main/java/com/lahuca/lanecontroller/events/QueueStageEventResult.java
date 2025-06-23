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
import com.lahuca.lane.queue.QueueType;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines the base class for the result for the queue stage failed event.
 */
public sealed interface QueueStageEventResult permits QueueStageEventResult.None, QueueStageEventResult.Disconnect,
        QueueStageEventResult.JoinInstance, QueueStageEventResult.JoinGame {

    /**
     * Result that tells the queue system that this is not the final stage of the event.
     */
    sealed interface QueueStageEventMessageableResult permits None, Disconnect {

        Optional<Component> getMessage();

    }

    /**
     * Result that tells the queue system that this is not the final stage of the event.
     */
    sealed interface QueueStageEventStageableResult permits JoinInstance, JoinGame {

        QueueStage constructStage(QueueStageResult reason, QueueType queueType);

    }

    sealed interface QueueStageEventJoinableResult permits JoinInstance, JoinGame {

        /**
         * The players (in UUID form) that should also try to join the given instance ID.
         * @return A set of UUIDs of the players that should also join the given instance ID.
         */
        HashSet<UUID> getJoinTogetherPlayers();

        /**
         * The type of queue it has been joined
         * @return The queue type
         */
        QueueType getQueueType();

    }

    /**
     * The class consisting of the data for doing nothing with the event.
     * This should be set when the player should stay at its server and the queue should be closed.
     * An additional message can be provided to be sent to the player is possible, useful when trying to join a different instance/game which fails.
     */
    record None(Component message) implements QueueStageEventResult, QueueStageEventResult.QueueStageEventMessageableResult {

        public None() {
            this(null);
        }

        @Override
        public Optional<Component> getMessage() {
            return Optional.ofNullable(message);
        }

    }

    /**
     * The class consisting of the data for a disconnect result.
     * The queue request will be closed.
     */
    record Disconnect(Component message) implements QueueStageEventResult, QueueStageEventResult.QueueStageEventMessageableResult {

        public Disconnect() {
            this(null);
        }

        @Override
        public Optional<Component> getMessage() {
            return Optional.ofNullable(message);
        }

    }

    /**
     * The class consisting of the data for an instance join result.
     * It also holds any other players (in UUID form) that should also do the same interaction.
     */
    record JoinInstance(String instanceId, HashSet<UUID> joinTogetherPlayers, QueueType queueType) implements QueueStageEventResult, QueueStageEventStageableResult, QueueStageEventJoinableResult {

        public JoinInstance(String instanceId) {
            this(instanceId, null, QueueType.PLAYING);
        }

        @Override
        public QueueStage constructStage(QueueStageResult reason, QueueType queueType) {
            return new QueueStage(reason, queueType, instanceId, null);
        }

        @Override
        public HashSet<UUID> getJoinTogetherPlayers() {
            return joinTogetherPlayers;
        }

        @Override
        public QueueType getQueueType() {
            return queueType;
        }

    }

    /**
     * The class consisting of the data for a game join result.
     * It also holds any other players (in UUID form) that should also do the same interaction.
     */
    record JoinGame(long gameId, HashSet<UUID> joinTogetherPlayers, QueueType queueType) implements QueueStageEventResult, QueueStageEventStageableResult, QueueStageEventJoinableResult {

        public JoinGame(long gameId) {
            this(gameId, null, QueueType.PLAYING);
        }

        @Override
        public QueueStage constructStage(QueueStageResult reason, QueueType queueType) {
            return new QueueStage(reason, queueType, null, gameId);
        }

        /**
         * The players (in UUID form) that should also try to join the given instance ID.
         * @return A set of UUIDs of the players that should also join the given instance ID.
         */
        @Override
        public HashSet<UUID> getJoinTogetherPlayers() {
            return joinTogetherPlayers;
        }

        @Override
        public QueueType getQueueType() {
            return queueType;
        }

    }

}
