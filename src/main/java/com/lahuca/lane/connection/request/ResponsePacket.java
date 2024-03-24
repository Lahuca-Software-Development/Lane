package com.lahuca.lane.connection.request;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/

public interface ResponsePacket<T> extends RequestPacket {

    String OK = "ok";
    String NOT_JOINABLE = "notJoinable";
    String NO_FREE_SLOTS = "noFreeSlots";

    String getResult();
    T getData();

}
