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
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import org.jetbrains.annotations.NotNull;

/**
 * Retrieves a list of DataObjects from the given table that match the version.
 *
 * @param requestId the request id to give the response to
 * @param prefix the prefix ID. This cannot be null, its values can be null.
 * @param permissionKey the permission key
 * @param version the version to match, null if no version is required
 */
public record DataObjectsListPacket(long requestId, @NotNull DataObjectId prefix, PermissionKey permissionKey, Integer version) implements RequestPacket {

    public static final String packetId = "dataObjectsList";

    static {
        Packet.registerPacket(packetId, DataObjectsListPacket.class);
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