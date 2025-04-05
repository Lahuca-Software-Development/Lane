package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;

public class RequestInformationPacket {

    public record Players(long requestId) implements RequestPacket {

        public static final String packetId = "requestInformationPacket.players";

        static {
            Packet.registerPacket(packetId, Players.class);
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

    public record Games(long requestId) implements RequestPacket {

        public static final String packetId = "requestInformationPacket.games";

        static {
            Packet.registerPacket(packetId, Games.class);
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

    public record Instances(long requestId) implements RequestPacket {

        public static final String packetId = "requestInformationPacket.instances";

        static {
            Packet.registerPacket(packetId, Instances.class);
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
