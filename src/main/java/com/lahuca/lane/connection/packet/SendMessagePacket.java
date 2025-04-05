package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;

import java.util.UUID;

/**
 * A packet that is being sent to the Controller to send the given player a message.
 * It must be noted that the message contains a JSON string of a Component.
 * TranslatableComponents will use the translations provided by the Controller.
 *
 * @param player the player's UUID
 * @param message the message
 */
public record SendMessagePacket(UUID player, String message) implements Packet {

    public static final String packetId = "sendMessage";

    static {
        Packet.registerPacket(packetId, SendMessagePacket.class);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }

}
