/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 17-3-2024 at 17:45 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection;

import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.packet.connection.ConnectionClosePacket;
import com.lahuca.lane.connection.packet.connection.ConnectionConnectPacket;
import com.lahuca.lane.connection.packet.connection.ConnectionKeepAlivePacket;
import com.lahuca.lane.connection.packet.connection.ConnectionKeepAliveResultPacket;
import com.lahuca.lane.connection.packet.data.DataObjectReadPacket;
import com.lahuca.lane.connection.packet.data.DataObjectRemovePacket;
import com.lahuca.lane.connection.packet.data.DataObjectWritePacket;
import com.lahuca.lane.connection.request.result.DataObjectResultPacket;
import com.lahuca.lane.connection.request.result.LongResultPacket;
import com.lahuca.lane.connection.request.result.SimpleResultPacket;
import com.lahuca.lane.connection.request.result.VoidResultPacket;

import java.util.HashMap;
import java.util.Optional;

public interface Packet {

	HashMap<String, Class<? extends Packet>> packetTypes = new HashMap<>();

	static void registerPacket(String typeId, Class<? extends Packet> classType) {
		packetTypes.put(typeId, classType);
	}

	static Optional<Class<? extends Packet>> getPacket(String typeId) {
		return Optional.ofNullable(packetTypes.get(typeId));
	}

	static void registerPackets() {
		Packet.registerPacket(GameStatusUpdatePacket.packetId, GameStatusUpdatePacket.class);
		Packet.registerPacket(InstanceDisconnectPacket.packetId, InstanceDisconnectPacket.class);
		Packet.registerPacket(InstanceJoinPacket.packetId, InstanceJoinPacket.class);
		Packet.registerPacket(InstanceStatusUpdatePacket.packetId, InstanceStatusUpdatePacket.class);
		Packet.registerPacket(InstanceUpdatePlayerPacket.packetId, InstanceUpdatePlayerPacket.class);
		Packet.registerPacket(PartyPacket.Player.Add.packetId, PartyPacket.Player.Add.class);
		Packet.registerPacket(PartyPacket.Player.Remove.packetId, PartyPacket.Player.Remove.class);
		Packet.registerPacket(PartyPacket.Disband.Request.packetId, PartyPacket.Disband.Request.class);
		Packet.registerPacket(PartyPacket.Retrieve.Request.packetId, PartyPacket.Retrieve.Request.class);
		Packet.registerPacket(PartyPacket.Retrieve.Response.packetId, PartyPacket.Retrieve.Response.class);
		Packet.registerPacket(QueueRequestPacket.packetId, QueueRequestPacket.class);
		Packet.registerPacket(RelationshipPacket.Create.Request.packetId, RelationshipPacket.Create.Request.class);
		Packet.registerPacket(RelationshipPacket.Retrieve.Request.packetId, RelationshipPacket.Retrieve.Request.class);
		Packet.registerPacket(RelationshipPacket.Retrieve.Response.packetId, RelationshipPacket.Retrieve.Response.class);
		Packet.registerPacket(ConnectionConnectPacket.packetId, ConnectionConnectPacket.class);

		Packet.registerPacket(SimpleResultPacket.packetId, SimpleResultPacket.class);
		Packet.registerPacket(VoidResultPacket.packetId, VoidResultPacket.class);
		Packet.registerPacket(LongResultPacket.packetId, LongResultPacket.class);
		Packet.registerPacket(DataObjectResultPacket.packetId, DataObjectResultPacket.class);

		Packet.registerPacket(QueueFinishedPacket.packetId, QueueFinishedPacket.class);
		Packet.registerPacket(ConnectionClosePacket.packetId, ConnectionClosePacket.class);
		Packet.registerPacket(ConnectionKeepAlivePacket.packetId, ConnectionKeepAlivePacket.class);
		Packet.registerPacket(ConnectionKeepAliveResultPacket.packetId, ConnectionKeepAliveResultPacket.class);
		Packet.registerPacket(RequestIdPacket.packetId, RequestIdPacket.class);

		Packet.registerPacket(DataObjectReadPacket.packetId, DataObjectReadPacket.class);
		Packet.registerPacket(DataObjectWritePacket.packetId, DataObjectWritePacket.class);
		Packet.registerPacket(DataObjectRemovePacket.packetId, DataObjectRemovePacket.class);
	}

	String getPacketId();

}
