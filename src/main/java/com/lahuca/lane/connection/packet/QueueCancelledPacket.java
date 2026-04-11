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
import com.lahuca.lane.queue.QueueRequest;

import java.util.UUID;

/**
 * Tells the instance that the queue of a player is cancelled.
 * @param player the player
 * @param queue the total queue request
 * @param disconnected whether the player has disconnected the network or if nothing has happened
 */
public record QueueCancelledPacket(UUID player, QueueRequest queue, boolean disconnected) implements Packet {

    public static final String packetId = "queueCancelled";

    static {
        Packet.registerPacket(packetId, QueueCancelledPacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }

}
