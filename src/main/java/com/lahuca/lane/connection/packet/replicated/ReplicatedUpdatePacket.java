package com.lahuca.lane.connection.packet.replicated;

/**
 * A packet involving updating a replicated object.
 *
 * @param <T> The type of the replication ID.
 * @param <R> The value type of the object being updated.
 */
public interface ReplicatedUpdatePacket<T, R> extends ReplicatedPacket<T> {

    /**
     * Returns the value of the object being updated.
     *
     * @return the value
     */
    R value();

}
