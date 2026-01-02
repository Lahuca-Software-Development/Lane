package com.lahuca.lane.events;

/**
 * An event involving subscribing a replicated object.
 *
 * @param <T> The type of the replication ID.
 */
public interface LaneReplicatedSubscribeEvent<T> extends LaneReplicatedEvent<T> {

    /**
     * Returns the ID of the subscriber.
     *
     * @return the subscriber ID
     */
    String getSubscriber();

}
