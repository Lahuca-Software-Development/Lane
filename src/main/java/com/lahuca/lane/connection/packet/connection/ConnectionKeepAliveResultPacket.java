/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 25-3-2024 at 00:41 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection.packet.connection;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.ResponsePacket;

public record ConnectionKeepAliveResultPacket(long requestId, String result) implements ResponsePacket<Void>, ConnectionPacket {

    public static final String packetId = "connectionKeepAliveResult";

    public static ConnectionKeepAliveResultPacket ok(long requestId) {
        return new ConnectionKeepAliveResultPacket(requestId, ResponsePacket.OK);
    }

    static {
        Packet.registerPacket(packetId, ConnectionKeepAliveResultPacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public Void getData() {
        return null;
    }
}
