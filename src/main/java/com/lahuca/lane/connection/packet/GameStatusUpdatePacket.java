/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 18-3-2024 at 16:33 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection.packet;

import com.lahuca.lane.LaneGameState;
import com.lahuca.lane.connection.Packet;

import java.util.UUID;

public record GameStatusUpdatePacket(UUID gameId, String name, LaneGameState state) implements Packet {

	public static final String packetId = "gameStatusUpdate";

	static {
		Packet.registerPacket(packetId, GameStatusUpdatePacket.class);
	}

	@Override
	public String getPacketId() {
		return packetId;
	}

}