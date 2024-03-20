package com.lahuca.lane.connection;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/

public interface ResponsePacket<T> extends Packet {

    long getRequestId();
    T getData();
}
