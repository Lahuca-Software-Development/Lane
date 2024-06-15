package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.records.PartyRecord;

import java.util.UUID;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/
public class PartyPacket {

    public static class Player {

        public record Add(long requestId, long partyId, UUID player) implements RequestPacket {

            public static final String packetId = "partyAddPlayerRequest";

            static {
                Packet.registerPacket(packetId, Player.Add.class);
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


        public record Remove(long requestId, long partyId, UUID player) implements RequestPacket {

            public static final String packetId = "partyRemovePlayerRequest";

            static {
                Packet.registerPacket(packetId, Player.Remove.class);
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

    public static class Disband {

        public record Request(long requestId, long partyId) implements RequestPacket {

            public static final String packetId = "partyDisbandRequest";

            static {
                Packet.registerPacket(packetId, Request.class);
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

    public static class Retrieve {

        public record Request(long requestId, long partyId) implements RequestPacket {

            public static final String packetId = "partyRetrieveRequest";

            static {
                Packet.registerPacket(packetId, Request.class);
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

        public record Response(long requestId, String result, PartyRecord partyRecord) implements ResponsePacket<PartyRecord> {

            public static final String packetId = "partyRetrieveResponse";

            static {
                Packet.registerPacket(packetId, Response.class);
            }

            public Response(long requestId, String result) {
                this(requestId, result, null);
            }

            @Override
            public String getPacketId() {
                return packetId;
            }

            @Override
            public long getRequestId() {
                return requestId;
            }

            @Override
            public String getResult() {
                return result;
            }

            @Override
            public PartyRecord getData() {
                return partyRecord;
            }
        }
    }
}
