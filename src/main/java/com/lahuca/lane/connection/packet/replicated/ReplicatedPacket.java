package com.lahuca.lane.connection.packet.replicated;

/**
 * Represents a packet involving a replicated object.
 * If a packet sent from the Instance requests to apply a change to the object, this is not extended.
 * Only if the packet actually consists of data that has affected the object.
 *
 * @param <T> The type of the replication ID.
 */
public interface ReplicatedPacket<T> {

    /**
     * Returns the ID that identifies this object.
     * This only identifies the object within the same object type: e.g. different parties.
     *
     * @return the replication ID
     */
    T getReplicationId();

}
