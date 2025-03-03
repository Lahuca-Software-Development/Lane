/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 24-2-2025 at 23:36 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2025
 */
package com.lahuca.lane.connection.packet.connection;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;

public record ConnectionKeepAlivePacket(long requestId, long requestTime) implements RequestPacket, ConnectionPacket {

    public static final String packetId = "connectionKeepAlive";

    static {
        Packet.registerPacket(packetId, ConnectionKeepAlivePacket.class);
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

    @Override
    public String getPacketId() {
        return packetId;
    }

}
