package com.lahuca.lane.connection;

import java.util.UUID;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/

public interface RequestablePacket<T> extends Packet {

    long getRequestId();
    UUID getDataId();
    T getData();
}
