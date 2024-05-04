package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.records.PlayerRecord;

/**
 * @author _Neko1
 * @date 04.05.2024
 **/

public record InstanceUpdatePlayerPacket(PlayerRecord playerRecord) implements Packet {

    public static final String packetId = "instanceUpdatePlayer";

    static {
        Packet.registerPacket(packetId, InstanceStatusUpdatePacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }
}
