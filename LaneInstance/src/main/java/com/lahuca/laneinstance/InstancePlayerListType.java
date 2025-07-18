package com.lahuca.laneinstance;

import com.lahuca.lane.queue.QueueType;

public enum InstancePlayerListType {

    NONE, RESERVED, ONLINE, PLAYERS, PLAYING;

    public static InstancePlayerListType fromQueueType(QueueType value) {
        return switch (value) {
            case ONLINE -> ONLINE;
            case PLAYERS -> PLAYERS;
            case PLAYING -> PLAYING;
            default -> NONE;
        };
    }

}
