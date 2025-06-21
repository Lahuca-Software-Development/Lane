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
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Defines the base class for the result for the queue stage failed event.
 */
public sealed class QueueStageEventResult permits QueueStageEventResult.None, QueueStageEventResult.Disconnect,
        QueueStageEventResult.JoinInstance, QueueStageEventResult.JoinGame {

    /**
     * Result that tells the queue system that this is not the final stage of the event.
     */
    public sealed interface QueueStageEventMessageableResult permits None, Disconnect {

        Component getMessage();

    }

    /**
     * Result that tells the queue system that this is not the final stage of the event.
     */
    public sealed interface QueueStageEventStageableResult permits JoinInstance, JoinGame {

        QueueStage constructStage(QueueStageResult reason);

    }

    public sealed interface QueueStageEventJoinableResult permits JoinInstance, JoinGame {

        /**
         * The players (in UUID form) that should also try to join the given instance ID.
         * @return A set of UUIDs of the players that should also join the given instance ID.
         */
        HashSet<UUID> getJoinTogetherPlayers();

    }

    /**
     * The class consisting of the data for doing nothing with the event.
     * This should be set when the player should stay at its server and the queue should be closed.
     * An additional message can be provided to be sent to the player is possible, useful when trying to join a different instance/game which fails.
     */
    public final static class None extends QueueStageEventResult implements QueueStageEventResult.QueueStageEventMessageableResult {

        private final Component message;

        public None() {
            this(null);
        }

        public None(Component message) {
            this.message = message;
        }

        @Override
        public Component getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", None.class.getSimpleName() + "[", "]")
                    .add("message=" + message)
                    .toString();
        }
    }

    /**
     * The class consisting of the data for a disconnect result.
     * The queue request will be closed.
     */
    public final static class Disconnect extends QueueStageEventResult implements QueueStageEventResult.QueueStageEventMessageableResult {

        private final Component message;

        public Disconnect() {
            this(null);
        }

        public Disconnect(Component message) {
            this.message = message;
        }

        @Override
        public Component getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Disconnect.class.getSimpleName() + "[", "]")
                    .add("message=" + message)
                    .toString();
        }
    }

    /**
     * The class consisting of the data for an instance join result.
     * It also holds any other players (in UUID form) that should also do the same interaction.
     */
    public final static class JoinInstance extends QueueStageEventResult implements QueueStageEventStageableResult, QueueStageEventJoinableResult {

        private final String instanceId;
        private final HashSet<UUID> joinTogetherPlayers;

        public JoinInstance(String instanceId) {
            this(instanceId, null);
        }

        public JoinInstance(String instanceId, HashSet<UUID> joinTogetherPlayers) {
            this.instanceId = instanceId;
            this.joinTogetherPlayers = joinTogetherPlayers;
        }

        public String getInstanceId() {
            return instanceId;
        }



        @Override
        public QueueStage constructStage(QueueStageResult reason) {
            return new QueueStage(reason, instanceId, null);
        }

        @Override
        public HashSet<UUID> getJoinTogetherPlayers() {
            return joinTogetherPlayers;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", JoinInstance.class.getSimpleName() + "[", "]")
                    .add("instanceId='" + instanceId + "'")
                    .add("joinTogetherPlayers=" + joinTogetherPlayers)
                    .toString();
        }
    }

    /**
     * The class consisting of the data for a game join result.
     * It also holds any other players (in UUID form) that should also do the same interaction.
     */
    public final static class JoinGame extends QueueStageEventResult implements QueueStageEventStageableResult, QueueStageEventJoinableResult {

        private final long gameId;
        private final HashSet<UUID> joinTogetherPlayers;

        public JoinGame(long gameId) {
            this(gameId, null);
        }

        public JoinGame(long gameId, HashSet<UUID> joinTogetherPlayers) {
            this.gameId = gameId;
            this.joinTogetherPlayers = joinTogetherPlayers;
        }

        public long getGameId() {
            return gameId;
        }

        @Override
        public QueueStage constructStage(QueueStageResult reason) {
            return new QueueStage(reason, null, gameId);
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
        public String toString() {
            return new StringJoiner(", ", JoinGame.class.getSimpleName() + "[", "]")
                    .add("gameId=" + gameId)
                    .add("joinTogetherPlayers=" + joinTogetherPlayers)
                    .toString();
        }
    }

}
