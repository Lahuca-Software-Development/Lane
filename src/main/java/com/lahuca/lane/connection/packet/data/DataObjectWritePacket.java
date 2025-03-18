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
package com.lahuca.lane.connection.packet.data;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.PermissionKey;

/**
 * A packet that tells the controller to write a data object.
 * @param requestId the request id to give the response to
 * @param object the data object to write
 * @param permissionKey the permission key
 */
public record DataObjectWritePacket(long requestId, DataObject object, PermissionKey permissionKey) implements RequestPacket {

	public static final String packetId = "dataObjectWrite";

	static {
		Packet.registerPacket(packetId, DataObjectWritePacket.class);
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