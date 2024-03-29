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
package com.lahuca.lane.connection.socket;

import com.lahuca.lane.connection.Packet;

public record SocketConnectPacket(String clientId) implements Packet {

	public static final String packetId = "socketConnect";

	static {
		Packet.registerPacket(packetId, SocketConnectPacket.class);
	}

	@Override
	public String getPacketId() {
		return packetId;
	}


}
