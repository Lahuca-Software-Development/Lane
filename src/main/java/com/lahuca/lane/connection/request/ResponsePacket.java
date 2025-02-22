package com.lahuca.lane.connection.request;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/

public interface ResponsePacket<T> extends RequestPacket {

    String OK = "ok";
    String OK_PARTIALLY = "okPartially";
    String UNKNOWN = "unknown";
    String NOT_JOINABLE = "notJoinable";
    String NO_FREE_SLOTS = "noFreeSlots";
    String INVALID_PARAMETERS = "invalidParameters";
    String INVALID_ID = "invalidId";
    String INVALID_PLAYER = "invalidPlayer";
    String INVALID_STATE = "invalidState";
    String INSUFFICIENT_RIGHTS = "insufficientRights";
    String CONNECTION_CANCELLED = "connectCancelled";
    String CONNECTION_IN_PROGRESS = "connectionInProgress";
    String CONNECTION_DISCONNECTED = "connectionDisconnected";

    String getResult();
    T getData();

    default Result<T> transformResult() {
        return new Result<>(this);
    }

}
