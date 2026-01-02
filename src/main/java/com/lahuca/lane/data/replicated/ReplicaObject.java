package com.lahuca.lane.data.replicated;

import com.lahuca.lane.records.RecordApplier;

/**
 * This is applied on a replicated object, where its replicas lie.
 *
 * @param <T> The type of the ID
 * @param <R> The type of the record to sync
 */
public interface ReplicaObject<T, R extends Record> extends ReplicatedObject<T>, RecordApplier<R> {

    /**
     * Returns the last synced time with the authoritative.
     *
     * @return the time
     */
    long getLastSyncReplicatedTime();

    /**
     * Returns whether this replica is subscribed to the authoritative.
     *
     * @return {@code true} if it is, otherwise {@code false}
     */
    boolean isSubscribed();

    /**
     * Subscribes to the authoritative.
     */
    void subscribeReplicated(); // TODO CompletableFuture

    /**
     * Unsubscribes from the authoritative.
     */
    void unsubscribeReplicated(); // TODO CompletableFuture

    /**
     * Returns whether the replicated object has been removed from the authoritative.
     *
     * @return {@code true} if it has, otherwise {@code false}
     */
    boolean isRemovedReplicated();

}
