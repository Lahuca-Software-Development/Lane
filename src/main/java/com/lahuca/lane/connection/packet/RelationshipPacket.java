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

    public record Request(long requestId, long relationshipId) implements RequestPacket {

        public static final String packetId = "requestRelationship";

        static {
            Packet.registerPacket(packetId, RelationshipPacket.Request.class);
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

    public record Response(long requestId, RelationshipRecord relationshipRecord) implements ResponsePacket<RelationshipRecord> {

        public static final String packetId = "responseRelationship";

        static {
            Packet.registerPacket(packetId, RelationshipPacket.Response.class);
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
            return null;
        }

        @Override
        public RelationshipRecord getData() {
            return relationshipRecord;
        }
    }

}
