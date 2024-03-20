/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 19-3-2024 at 13:13 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.records.PlayerRecord;

import java.util.UUID;

/**
 * A packet that tells an instance that the given players are joining the instance.
 * If the game ID is not null, the players are meant to join the given game. Otherwise a global lobby.
 * If the game has the possibility, the players should be grouped together if applicable.
 * @param players the players to join the instance
 * @param gameId the game id to join, null if only joining the instance
 */
public record InstanceJoinPacket(PlayerRecord[] players, UUID gameId) implements Packet {

	public static final String packetId = "instanceJoin";

	static {
		Packet.registerPacket(packetId, InstanceJoinPacket.class);
	}

	@Override
	public String getPacketId() {
		return packetId;
	}

}