/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 18-3-2024 at 13:36 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection.packet.connection;

import com.lahuca.lane.connection.Packet;

public record ConnectionConnectPacket(String clientId) implements Packet, ConnectionPacket {

	public static final String packetId = "connectionConnect";

	static {
		Packet.registerPacket(packetId, ConnectionConnectPacket.class);
	}

	@Override
	public String getPacketId() {
		return packetId;
	}


}
