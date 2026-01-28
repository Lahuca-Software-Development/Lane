package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.ResponseError;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.InstanceRecord;
import com.lahuca.lane.records.PlayerRecord;

import java.util.ArrayList;
import java.util.UUID;

/**
 * This class holds information about packets that request and respond about information on the controller.
 * The single packets Player, Game and Instance are used to request a specific player, game or instance.
 * The response packets are PlayerResponse, GameResponse and InstanceResponse.
 * To retrieve all of them, use the plural versions: Players, Games and Instances.
 */
public class RequestInformationPacket {

    public record Player(long requestId, UUID uuid) implements RequestPacket {

        public static final String packetId = "requestInformationPacket.player";

        static {
            Packet.registerPacket(packetId, Player.class);
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

    public record PlayerResponse(long requestId, ResponseError error, PlayerRecord data) implements ResponsePacket<PlayerRecord> {

        public static final String packetId = "requestInformationPacket.playerResponse";

        static {
            Packet.registerPacket(packetId, PlayerResponse.class);
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
        public ResponseError getError() {
            return error;
        }

        @Override
        public PlayerRecord getData() {
            return data;
        }

    }

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

    public record PlayersResponse(long requestId, ResponseError error, ArrayList<PlayerRecord> data) implements ResponsePacket<ArrayList<PlayerRecord>> {

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
        public ResponseError getError() {
            return error;
        }

        @Override
        public ArrayList<PlayerRecord> getData() {
            return data;
        }

    }

    public record Game(long requestId, long gameId) implements RequestPacket {

        public static final String packetId = "requestInformationPacket.game";

        static {
            Packet.registerPacket(packetId, Game.class);
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

    public record GameResponse(long requestId, ResponseError error, GameRecord data) implements ResponsePacket<GameRecord> {

        public static final String packetId = "requestInformationPacket.gameResponse";

        static {
            Packet.registerPacket(packetId, GameResponse.class);
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
        public ResponseError getError() {
            return error;
        }

        @Override
        public GameRecord getData() {
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

    public record GamesResponse(long requestId, ResponseError error, ArrayList<GameRecord> data) implements ResponsePacket<ArrayList<GameRecord>> {

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
        public ResponseError getError() {
            return error;
        }

        @Override
        public ArrayList<GameRecord> getData() {
            return data;
        }

    }

    public record Instance(long requestId, String id) implements RequestPacket {

        public static final String packetId = "requestInformationPacket.instance";

        static {
            Packet.registerPacket(packetId, Instance.class);
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

    public record InstanceResponse(long requestId, ResponseError error, InstanceRecord data) implements ResponsePacket<InstanceRecord> {

        public static final String packetId = "requestInformationPacket.instanceResponse";

        static {
            Packet.registerPacket(packetId, InstanceResponse.class);
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
        public ResponseError getError() {
            return error;
        }

        @Override
        public InstanceRecord getData() {
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

    public record InstancesResponse(long requestId, ResponseError error, ArrayList<InstanceRecord> data) implements ResponsePacket<ArrayList<InstanceRecord>> {

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
        public ResponseError getError() {
            return error;
        }

        @Override
        public ArrayList<InstanceRecord> getData() {
            return data;
        }

    }

    public record PlayerUsername(long requestId, UUID uuid) implements RequestPacket {

        public static final String packetId = "requestInformationPacket.playerUsername";

        static {
            Packet.registerPacket(packetId, PlayerUsername.class);
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

    public record PlayerUuid(long requestId, String username) implements RequestPacket {

        public static final String packetId = "requestInformationPacket.playerUuid";

        static {
            Packet.registerPacket(packetId, PlayerUuid.class);
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

    public record PlayerNetworkProfile(long requestId, UUID uuid) implements RequestPacket {

        public static final String packetId = "requestInformationPacket.playerNetworkProfile";

        static {
            Packet.registerPacket(packetId, PlayerNetworkProfile.class);
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
