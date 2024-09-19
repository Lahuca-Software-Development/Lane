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
     * The player has joined the network and is looking for its first server.
     */
    NETWORK_JOIN,
    /**
     * The player has joined the queue due to it being in a party.
     */
    PARTY_JOIN,
    /**
     * The player is being kicked from the server and is therefore requesting a new one.
     */
    SERVER_KICK;

}
