package com.lahuca.lane.connection.packet.replicated;

/**
 * A packet involving subscribing a replicated object.
 *
 * @param <T> The type of the replication ID.
 */
public interface ReplicatedRemovePacket<T> extends ReplicatedPacket<T> {

}
