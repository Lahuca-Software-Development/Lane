package com.lahuca.lane.data.replicated;

/**
 * Represents an object that can be replicated.
 * This is applied on the main object, not on the specific object.
 *
 * @param <T> The type of the replication ID.
 */
public interface ReplicatedObject<T> {

    /**
     * Returns the ID that identifies this object.
     * This only identifies the object within the same object type: e.g. different parties.
     *
     * @return the replication ID
     */
    T getReplicationId();

}
