package com.lahuca.lane.connection.packet;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.connection.Packet;

import java.util.UUID;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/
public record PlayerQuitGamePacket(LaneParty player, UUID gameId) implements Packet {

    public static final String packetId = "playerQuitGame";

    static {
        Packet.registerPacket(packetId, PlayerQuitGamePacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }

}
