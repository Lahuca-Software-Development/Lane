package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.data.profile.ProfileType;

import java.util.UUID;

public interface ProfilePacket {

    record GetProfileData(long requestId, UUID uuid) implements RequestPacket, ProfilePacket {

        public static final String packetId = "profileGetProfileData";

        static {
            Packet.registerPacket(packetId, ProfilePacket.GetProfileData.class);
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

    record CreateNew(long requestId, ProfileType type) implements RequestPacket, ProfilePacket {

        public static final String packetId = "profileCreateNew";

        static {
            Packet.registerPacket(packetId, ProfilePacket.CreateNew.class);
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

    record AddSubProfile(long requestId, UUID current, UUID subProfile,
                         String name, boolean active) implements RequestPacket, ProfilePacket {

        public static final String packetId = "profileAddSubProfile";

        static {
            Packet.registerPacket(packetId, ProfilePacket.AddSubProfile.class);
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

    record RemoveSubProfile(long requestId, UUID current, UUID subProfile,
                            String name) implements RequestPacket, ProfilePacket {

        public static final String packetId = "profileSubProfilesRemove";

        static {
            Packet.registerPacket(packetId, ProfilePacket.RemoveSubProfile.class);
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

    record ResetDelete(long requestId, UUID current, boolean delete) implements RequestPacket, ProfilePacket {

        public static final String packetId = "profileResetDelete";

        static {
            Packet.registerPacket(packetId, ProfilePacket.ResetDelete.class);
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

    record Copy(long requestId, UUID current, UUID from) implements RequestPacket, ProfilePacket {

        public static final String packetId = "profileCopy";

        static {
            Packet.registerPacket(packetId, ProfilePacket.Copy.class);
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

    record SetNetworkProfile(long requestId, UUID player, UUID profile) implements RequestPacket, ProfilePacket {

        public static final String packetId = "profileSetNetworkProfile";

        static {
            Packet.registerPacket(packetId, ProfilePacket.SetNetworkProfile.class);
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