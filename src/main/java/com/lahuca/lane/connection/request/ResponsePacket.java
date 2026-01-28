package com.lahuca.lane.connection.request;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/

public interface ResponsePacket<T> extends RequestPacket {



    ResponseError getError();
    T getData();

    default ResponsePacket<Object> toObjectResponsePacket() {
        return new ResponsePacket<>() {
            @Override
            public ResponseError getError() {
                return ResponsePacket.this.getError();
            }

            @Override
            public Object getData() {
                return ResponsePacket.this.getData();
            }

            @Override
            public long requestId() {
                return ResponsePacket.this.requestId();
            }

            @Override
            public String getPacketId() {
                return ResponsePacket.this.getPacketId();
            }
        };
    }

}
