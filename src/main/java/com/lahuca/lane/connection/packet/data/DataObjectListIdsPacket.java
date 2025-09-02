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

/**
 * A packet to tell the controller to retrieve a list of data object IDs whose key has the same prefix from the provided ID (case sensitive).
 * Example for the input with id = "myPrefix" with relationalId = ("players", "Laurenshup"), it will return:
 *  <ul>
 *      <li>players.Laurenshup.myPrefix.value1</li>
 *      <li>players.Laurenshup.myPrefix.value2.subKey</li>
 *     <li>players.Laurenshup.myPrefixSuffix</li>
 * </ul>
 *
 * @param requestId the request id to give the response to
 * @param prefix    the prefix ID. This cannot be null, its values can be null.
 */
public record DataObjectListIdsPacket(long requestId, DataObjectId prefix) implements RequestPacket {

    public static final String packetId = "dataObjectListIds";

    static {
        Packet.registerPacket(packetId, DataObjectListIdsPacket.class);
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