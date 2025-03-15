/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 23-3-2024 at 12:20 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;

import java.util.UUID;

/**
 * Tells the controller that the queue of a player should be finished.
 * The request ID is used to give a response back to the instance that issues the finalization.
 * @param requestId The request ID for the response
 * @param player The player
 * @param gameId The game ID that has been joined, null when it has not.
 */
public record QueueFinishedPacket(long requestId, UUID player, Long gameId) implements RequestPacket {

    public static final String packetId = "queueFinished";

    static {
        Packet.registerPacket(packetId, QueueFinishedPacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

}
