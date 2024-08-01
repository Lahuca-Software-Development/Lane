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

/**
 * Defines the base class for the result for the queue stage failed event.
 */
public sealed class QueueStageEventResult permits QueueStageEventResult.None, QueueStageEventResult.Disconnect,
        QueueStageEventResult.QueueStageEventStageableResult {

    public sealed static abstract class QueueStageEventStageableResult extends QueueStageEventResult
            permits JoinInstance, JoinGame {

        public abstract QueueStage constructStage(QueueStageResult reason);

    }

    /**
     * The class consisting of the data for doing nothing with the event.
     * This should be set when the player should stay at its server and the queue should be closed.
     */
    public final static class None extends QueueStageEventResult {

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
     */
    public final static class JoinInstance extends QueueStageEventStageableResult {

        private final String instanceId;

        public JoinInstance(String instanceId) {
            this.instanceId = instanceId;
        }

        public String getInstanceId() {
            return instanceId;
        }

        @Override
        public QueueStage constructStage(QueueStageResult reason) {
            return new QueueStage(reason, instanceId, null);
        }
    }

    /**
     * The class consisting of the data for a game join result.
     */
    public final static class JoinGame extends QueueStageEventStageableResult {

        private final long gameId;

        public JoinGame(long gameId) {
            this.gameId = gameId;
        }

        public long getGameId() {
            return gameId;
        }

        @Override
        public QueueStage constructStage(QueueStageResult reason) {
            return new QueueStage(reason, null, gameId);
        }
    }

}
