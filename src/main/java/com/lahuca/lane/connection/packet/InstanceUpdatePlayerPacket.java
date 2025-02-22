package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.records.PlayerRecord;

/**
 * Sends the instance an update about the player.
 * This should not be sent the other way around, i.e. to the controller.
 * The controller never changes full player objects.
 * @param playerRecord The record to send
 */
public record InstanceUpdatePlayerPacket(PlayerRecord playerRecord) implements Packet {

    public static final String packetId = "instanceUpdatePlayer";

    static {
        Packet.registerPacket(packetId, InstanceUpdatePlayerPacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }
}
