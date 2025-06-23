/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 30-7-2024 at 20:23 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.queue;

import net.kyori.adventure.text.Component;

import java.util.Optional;

/**
 * Contains the data for a stage that has been done.
 * The result represents what has happened for the stage to fail/succeed.
 * The other parameters represent what has tried to join.
 * @param result The result.
 * @param resultMessage an additional message that caused the result
 * @param queueType the queue type that was failed/succeeded
 * @param instanceId The ID of an instance to join, also the ID of the instance of the game.
 * @param gameId The ID of a game to join.
 */
public record QueueStage(QueueStageResult result, Component resultMessage, QueueType queueType, String instanceId, Long gameId) {

    public QueueStage(QueueStageResult result, QueueType queueType, String instanceId, Long gameId) {
        this(result, null, queueType, instanceId, gameId);
    }

    public Optional<Component> getResultMessage() {
        return Optional.ofNullable(resultMessage);
    }

}
