package com.lahuca.lane.connection.request;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/

public interface ResponsePacket<T> extends RequestPacket {

    T getData();

}
