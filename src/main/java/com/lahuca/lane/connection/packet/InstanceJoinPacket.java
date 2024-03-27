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
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.records.PlayerRecord;

/**
 * A packet that tells an instance that the given player is joining the instance.
 * If the game ID is not null, the player is meant to join the given game. Otherwise, a global lobby.
 * If the game has the possibility, the player should be grouped together if applicable.
 * @param player the player to join the instance
 * @param overrideSlots when there are no slots left for players, should we still allow to join
 * @param gameId the game id to join, null if only joining the instance
 */
public record InstanceJoinPacket(long requestId, PlayerRecord player, boolean overrideSlots, Long gameId) implements RequestPacket {

	public static final String packetId = "instanceJoin";

	static {
		Packet.registerPacket(packetId, InstanceJoinPacket.class);
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