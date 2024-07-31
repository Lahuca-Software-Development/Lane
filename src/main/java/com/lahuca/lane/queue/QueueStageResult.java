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

/**
 * These enum values represent the result the associated {@link QueueStage} has been returned.
 */
public enum QueueStageResult {

    /**
     * The given ID could not be identified to a game or instance.
     */
    UNKNOWN_ID,
    /**
     * The given instance/game has no slots left or is not joinable.
     */
    NOT_JOINABLE,
    /**
     * The given instance/game did not return a response.
     */
    NO_RESPONSE,
    /**
     * The given instance/game did not accept the join.
     */
    JOIN_DENIED,
    /**
     * The proxy cannot connect the player with the instance/game.
     */
    SERVER_UNAVAILABLE,
    /**
     * Data is not being handled properly somewhere.
     */
    INVALID_STATE;

}
