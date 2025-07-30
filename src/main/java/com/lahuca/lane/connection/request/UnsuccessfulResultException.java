package com.lahuca.lane.connection.request;

public class UnsuccessfulResultException extends RuntimeException {

    public UnsuccessfulResultException(String message) {
        super(message);
        if(message.equals(ResponsePacket.OK)) {
            throw new IllegalStateException("The result was OK, which is not allowed here.");
        }
    }

}
