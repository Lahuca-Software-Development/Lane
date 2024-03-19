package com.lahuca.lane.connection.packet;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.connection.Packet;

import java.util.UUID;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/
public record PlayerJoinGamePacket(LaneParty player, UUID gameId) implements Packet {

    public static final String packetId = "playerJoinGame";

    static {
        Packet.registerPacket(packetId, PlayerJoinGamePacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }
}
