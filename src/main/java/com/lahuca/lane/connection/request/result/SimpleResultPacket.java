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
import com.lahuca.lane.connection.request.ResponseError;
import com.lahuca.lane.connection.request.ResponsePacket;

public record SimpleResultPacket<T>(long requestId, ResponseError error, T data) implements ResponsePacket<T> {

    public static final String packetId = "simpleResult";

    static {
        Packet.registerPacket(packetId, SimpleResultPacket.class);
    }

    /**
     * Constructor for a result that is successful.
     * @param requestId the request ID
     * @param data the data
     */
    public SimpleResultPacket(long requestId, T data) {
        this(requestId, null, data);
    }

    /**
     * Constructor for a result that is unsuccessful.
     * @param requestId the request ID
     * @param error the error
     */
    public SimpleResultPacket(long requestId, ResponseError error) {
        this(requestId, error, null);
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
    public ResponseError getError() {
        return error;
    }

    @Override
    public T getData() {
        return data;
    }
}
