package com.lahuca.lane.connection.packet;

import com.lahuca.lane.FriendshipInvitation;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;

import java.util.UUID;

public interface FriendshipPacket {

    static void register() {
        Packet.registerPacket(GetInvitations.packetId, GetInvitations.class);
        Packet.registerPacket(ContainsInvitation.packetId, ContainsInvitation.class);
        Packet.registerPacket(InvalidateInvitation.packetId, InvalidateInvitation.class);
        Packet.registerPacket(Invite.packetId, Invite.class);
        Packet.registerPacket(AcceptInvitation.packetId, AcceptInvitation.class);
        Packet.registerPacket(GetFriendship.packetId, GetFriendship.class);
        Packet.registerPacket(GetFriendships.packetId, GetFriendships.class);
        Packet.registerPacket(RemoveFriendship.packetId, RemoveFriendship.class);
    }

    record GetInvitations(long requestId, UUID player, Boolean includeRequester, Boolean includeInvited) implements RequestPacket, FriendshipPacket {

        public static final String packetId = "friendshipGetInvitations";

        static {
            Packet.registerPacket(packetId, GetInvitations.class);
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

    record ContainsInvitation(long requestId, FriendshipInvitation invitation) implements RequestPacket, FriendshipPacket {

        public static final String packetId = "friendshipContainsInvitation";

        static {
            Packet.registerPacket(packetId, ContainsInvitation.class);
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

    record InvalidateInvitation(long requestId, FriendshipInvitation invitation) implements RequestPacket, FriendshipPacket {

        public static final String packetId = "friendshipInvalidateInvitation";

        static {
            Packet.registerPacket(packetId, InvalidateInvitation.class);
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

    record Invite(long requestId, FriendshipInvitation invitation, String username) implements RequestPacket, FriendshipPacket {

        public static final String packetId = "friendshipInvite";

        static {
            Packet.registerPacket(packetId, Invite.class);
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

    record AcceptInvitation(long requestId, FriendshipInvitation invitation) implements RequestPacket, FriendshipPacket {

        public static final String packetId = "friendshipAcceptInvitation";

        static {
            Packet.registerPacket(packetId, AcceptInvitation.class);
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

    record GetFriendship(long requestId, long friendshipId) implements RequestPacket, FriendshipPacket {

        public static final String packetId = "friendshipsGetFriendship";

        static {
            Packet.registerPacket(packetId, GetFriendship.class);
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

    record GetFriendships(long requestId, UUID player) implements RequestPacket, FriendshipPacket {

        public static final String packetId = "friendshipsGetFriendships";

        static {
            Packet.registerPacket(packetId, GetFriendships.class);
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

    record RemoveFriendship(long requestId, long friendshipId) implements RequestPacket, FriendshipPacket {

        public static final String packetId = "friendshipsRemoveFriendship";

        static {
            Packet.registerPacket(packetId, RemoveFriendship.class);
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