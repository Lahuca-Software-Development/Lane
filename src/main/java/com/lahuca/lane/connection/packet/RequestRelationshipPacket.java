package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.RequestablePacket;
import com.lahuca.lane.records.RelationshipRecord;

import java.util.UUID;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/
public record RequestRelationshipPacket(long requestId, UUID dataId,  RelationshipRecord relationshipRecord) implements RequestablePacket<RelationshipRecord> {

    public static final String packetId = "requestRelationship";

    static {
        Packet.registerPacket(packetId, RequestPartyPacket.class);
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
    public UUID getDataId() {
        return dataId;
    }

    @Override
    public RelationshipRecord getData() {
        return relationshipRecord;
    }
}
