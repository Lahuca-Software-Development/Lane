package com.lahuca.lane.events;

/**
 * An event involving unsubscribing a replicated object.
 *
 * @param <T> The type of the replication ID.
 */
public interface LaneReplicatedUnsubscribeEvent<T> extends LaneReplicatedEvent<T> {

    /**
     * Returns the ID of the subscriber that is unsubscribing.
     *
     * @return the subscriber ID
     */
    String getSubscriber();

}
