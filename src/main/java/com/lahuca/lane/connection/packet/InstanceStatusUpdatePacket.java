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

public record InstanceStatusUpdatePacket(boolean joinable, boolean nonPlayable, int maxPlayers) implements Packet {

    public static final String packetId = "instanceStatusUpdate";

    static {
        Packet.registerPacket(packetId, InstanceStatusUpdatePacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }

}
