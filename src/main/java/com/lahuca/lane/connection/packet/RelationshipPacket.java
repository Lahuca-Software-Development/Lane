package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.records.RelationshipRecord;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/
public class RelationshipPacket {


    public static class Create {

        public record Request(long requestId, long relationshipId) implements RequestPacket {

            public static final String packetId = "relationshipCreateRequest";

            static {
                Packet.registerPacket(packetId, RelationshipPacket.Create.Request.class);
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

        public record Request(long requestId, long relationshipId) implements RequestPacket {

            public static final String packetId = "relationshipRetrieveRequest";

            static {
                Packet.registerPacket(packetId, RelationshipPacket.Retrieve.Request.class);
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

        public record Response(long requestId, String result, RelationshipRecord relationshipRecord) implements ResponsePacket<RelationshipRecord> {

            public static final String packetId = "relationshipRetrieveResponse";

            static {
                Packet.registerPacket(packetId, RelationshipPacket.Retrieve.Response.class);
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
            public RelationshipRecord getData() {
                return relationshipRecord;
            }
        }

    }

}
