/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 25-3-2024 at 00:41 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection.request.result;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.records.ProfileRecord;

/**
 * Due to encoding/decoding of the used parser, generic types are parsed to {@link com.google.gson.internal.LinkedTreeMap}.
 * Using this result packet fixes that problem, by explicitly sending the result to be a {@link ProfileRecord}.
 * @param requestId the request id of the original request
 * @param result the result string
 * @param data the data
 */
public record ProfileRecordResultPacket(long requestId, String result, ProfileRecord data) implements ResponsePacket<ProfileRecord> {

    public static final String packetId = "profileRecordResult";

    static {
        Packet.registerPacket(packetId, ProfileRecordResultPacket.class);
    }

    /**
     * Constructor for a result that is successful.
     * @param requestId the request ID
     * @param data the data
     */
    public ProfileRecordResultPacket(long requestId, ProfileRecord data) {
        this(requestId, ResponsePacket.OK, data);
    }

    /**
     * Constructor for a result that is unsuccessful.
     * @param requestId the request ID
     * @param result the error
     */
    public ProfileRecordResultPacket(long requestId, String result) {
        this(requestId, result, null);
    }

    @Override
    public String getPacketId() {
        return packetId;
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public ProfileRecord getData() {
        return data;
    }
}
