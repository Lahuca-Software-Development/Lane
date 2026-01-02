package com.lahuca.lane.events;

/**
 * An event involving the removal of a replicated object.
 *
 * @param <T> The type of the replication ID.
 */
public interface LaneReplicatedRemoveEvent<T> extends LaneReplicatedEvent<T> {

}
