package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.InstanceRecord;
import com.lahuca.lane.records.PlayerRecord;

import java.util.ArrayList;

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

    public record PlayersResponse(long requestId, String result, ArrayList<PlayerRecord> data) implements ResponsePacket<ArrayList<PlayerRecord>> {

        public static final String packetId = "requestInformationPacket.playersResponse";

        static {
            Packet.registerPacket(packetId, PlayersResponse.class);
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
        public ArrayList<PlayerRecord> getData() {
            return data;
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

    public record GamesResponse(long requestId, String result, ArrayList<GameRecord> data) implements ResponsePacket<ArrayList<GameRecord>> {

        public static final String packetId = "requestInformationPacket.gamesResponse";

        static {
            Packet.registerPacket(packetId, GamesResponse.class);
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
        public ArrayList<GameRecord> getData() {
            return data;
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

    public record InstancesResponse(long requestId, String result, ArrayList<InstanceRecord> data) implements ResponsePacket<ArrayList<InstanceRecord>> {

        public static final String packetId = "requestInformationPacket.instancesResponse";

        static {
            Packet.registerPacket(packetId, InstancesResponse.class);
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
        public ArrayList<InstanceRecord> getData() {
            return data;
        }

    }

}
