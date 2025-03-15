package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;

public record RequestIdPacket(long requestId, Type type) implements RequestPacket {

    public static final String packetId = "requestId";

    static {
        Packet.registerPacket(packetId, RequestIdPacket.class);
    }

    public enum Type {
        GAME
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
