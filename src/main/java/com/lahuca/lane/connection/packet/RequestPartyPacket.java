package com.lahuca.lane.connection.packet;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.connection.Packet;

import java.util.UUID;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/
public record RequestPartyPacket(UUID inviter, LanePlayer requested) implements Packet {

    public static final String packetId = "requestParty";

    static {
        Packet.registerPacket(packetId, RequestPartyPacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }
}
