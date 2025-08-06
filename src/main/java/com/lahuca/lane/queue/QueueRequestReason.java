/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 30-7-2024 at 20:14 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.queue;

/**
 * The enum which enumerates all possible values for when a queue request might happen.
 */
public enum QueueRequestReason {

    /**
     * A plugin attached to the controller is starting a new queue request.
     */
    PLUGIN_CONTROLLER,
    /**
     * A plugin attached to an instance is starting a new queue request.
     */
    PLUGIN_INSTANCE,
    /**
     * The player has joined the network and is looking for its first server.
     * By default contains the lobby parameters {@link QueueRequestParameters#lobbyParameters}.
     */
    NETWORK_JOIN,
    /**
     * The player has joined the queue due to it being in a party.
     * By default contains the final instance ID and optionally the game ID of what has been joined by the party owner.
     */
    PARTY_JOIN,
    /**
     * The player is being kicked from the server and is therefore requesting a new one.
     * By default contains the lobby parameters {@link QueueRequestParameters#lobbyParameters}.
     */
    SERVER_KICKED,
    /**
     * The player is being kicked from the server and is therefore requesting a new one.
     * By default contains the lobby parameters {@link QueueRequestParameters#lobbyParameters}.
     */
    GAME_SHUTDOWN,
    /**
     * The player has quit a game due to a plugin or its system issuing the quit.
     * By default contains the lobby parameters {@link QueueRequestParameters#lobbyParameters}.
     */
    GAME_QUIT;


}
