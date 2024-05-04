package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.records.PlayerRecord;

/**
 * @author _Neko1
 * @date 04.05.2024
 **/

public record InstanceUpdatePlayerPacket(PlayerRecord playerRecord) implements Packet {

    public static final String packetId = "updatePlayer";

    static {
        Packet.registerPacket(packetId, InstanceUpdatePlayerPacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }
}
