package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.records.PartyRecord;

import java.util.UUID;

/**
 * This class holds the packets for transmitting information and operations between the controller and instance.
 * @author _Neko1
 * @date 19.03.2024
 **/
public class PartyPacket {

    // TODO Check Retrieve class
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

    // TODO New below

    public record SetInvitationsOnly(long requestId, long partyId, boolean invitationsOnly) implements RequestPacket {

        public static final String packetId = "partySetInvitationsOnly";

        static {
            Packet.registerPacket(packetId, SetInvitationsOnly.class);
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

    public static class Invitation {

        public record Has(long requestId, long partyId, UUID player) implements RequestPacket {

            public static final String packetId = "partyInvitationHas";

            static {
                Packet.registerPacket(packetId, Has.class);
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

        public record Add(long requestId, long partyId, UUID player) implements RequestPacket {

            public static final String packetId = "partyInvitationAdd";

            static {
                Packet.registerPacket(packetId, Add.class);
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

        public record Accept(long requestId, long partyId, UUID player) implements RequestPacket {

            public static final String packetId = "partyInvitationAccept";

            static {
                Packet.registerPacket(packetId, Accept.class);
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

        public record Deny(long requestId, long partyId, UUID player) implements RequestPacket {

            public static final String packetId = "partyInvitationDeny";

            static {
                Packet.registerPacket(packetId, Deny.class);
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

    public record JoinPlayer(long requestId, long partyId, UUID player) implements RequestPacket {

        public static final String packetId = "partyJoinPlayer";

        static {
            Packet.registerPacket(packetId, JoinPlayer.class);
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

    public record RemovePlayer(long requestId, long partyId, UUID player) implements RequestPacket {

        public static final String packetId = "partyRemovePlayer";

        static {
            Packet.registerPacket(packetId, RemovePlayer.class);
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

    public record Disband(long requestId, long partyId) implements RequestPacket {

        public static final String packetId = "partyDisband";

        static {
            Packet.registerPacket(packetId, Disband.class);
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

    public record SetOwner(long requestId, long partyId, UUID player) implements RequestPacket {

        public static final String packetId = "partySetOwner";

        static {
            Packet.registerPacket(packetId, SetOwner.class);
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

    public record Warp(long requestId, long partyId) implements RequestPacket {

        public static final String packetId = "partyWarp";

        static {
            Packet.registerPacket(packetId, Warp.class);
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
