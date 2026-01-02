package com.lahuca.lane.connection.packet.replicated;

/**
 * An event involving subscribing a replicated object.
 *
 * @param <T> The type of the replication ID.
 */
public interface ReplicatedSubscribePacket<T> extends ReplicatedPacket<T> {

}
