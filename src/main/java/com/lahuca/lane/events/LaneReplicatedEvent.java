package com.lahuca.lane.events;

/**
 * An event revolving a replicated object.
 *
 * @param <T> The type of the replication ID.
 */
public interface LaneReplicatedEvent<T> extends LaneEvent {

    /**
     * Returns the ID that identifies this object.
     * This only identifies the object within the same object type: e.g. different parties.
     *
     * @return the replication ID
     */
    T getReplicationId();

}
