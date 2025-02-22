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

import java.util.HashMap;
import java.util.Optional;

public interface Packet {

	HashMap<String, Class<? extends Packet>> packetTypes = new HashMap<>();

	static void registerPacket(String typeId, Class<? extends Packet> classType) {
		packetTypes.put(typeId, classType);
		System.out.println("Register Packet"+ typeId);
	}

	static Optional<Class<? extends Packet>> getPacket(String typeId) {
		return Optional.ofNullable(packetTypes.get(typeId));
	}

	String getPacketId();

}
