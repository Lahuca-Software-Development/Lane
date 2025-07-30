package com.lahuca.lane.connection.packet.data;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;

import java.util.UUID;

public class SavedLocalePacket {

    public record Get(long requestId, UUID networkProfile) implements RequestPacket {

        public static final String packetId = "savedLocaleGet";

        static {
            Packet.registerPacket(packetId, SavedLocalePacket.Get.class);
        }


        @Override
        public long getRequestId() {
            return requestId;
        }

        @Override
        public String getPacketId() {
            return packetId;
        }

    }

    public record Set(long requestId, UUID networkProfile, String locale) implements RequestPacket {

        public static final String packetId = "savedLocaleSet";

        static {
            Packet.registerPacket(packetId, SavedLocalePacket.Set.class);
        }

        @Override
        public long getRequestId() {
            return requestId;
        }

        @Override
        public String getPacketId() {
            return packetId;
        }

    }

}
