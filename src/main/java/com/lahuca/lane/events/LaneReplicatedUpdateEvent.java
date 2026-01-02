package com.lahuca.lane.events;

/**
 * An event involving the update of a replicated object.
 * Events that extend this, actually update the object.
 *
 * @param <I> The type of the replication ID.
 * @param <D> The object data type being updated.
 */
public interface LaneReplicatedUpdateEvent<I, D> extends LaneReplicatedEvent<I> {

    /**
     * Returns the data of the object being updated.
     *
     * @return the data
     */
    D getData();

}
