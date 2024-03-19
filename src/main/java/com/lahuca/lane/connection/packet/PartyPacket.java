package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.ResponsePacket;
import com.lahuca.lane.records.PartyRecord;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/
public class PartyPacket {

    public record Request(long requestId, long partyId) implements Packet {

        public static final String packetId = "requestParty";

        static {
            Packet.registerPacket(packetId, Request.class);
        }


        @Override
        public String getPacketId() {
            return packetId;
        }
    }

    public record Response(long requestId, PartyRecord partyRecord) implements ResponsePacket<PartyRecord> {

        public static final String packetId = "responseParty";

        static {
            Packet.registerPacket(packetId, Response.class);
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
        public PartyRecord getData() {
            return partyRecord;
        }
    }

}
