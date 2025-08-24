package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;

import java.util.UUID;

/**
 * This class holds information about packets that request and respond about setting information on the controller.
 */
public interface SetInformationPacket {

    record PlayerSetNickname(long requestId, UUID uuid, String nickname) implements RequestPacket, SetInformationPacket {

        public static final String packetId = "setInformationPacket.playerSetNickname";

        static {
            Packet.registerPacket(packetId, PlayerSetNickname.class);
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

}
