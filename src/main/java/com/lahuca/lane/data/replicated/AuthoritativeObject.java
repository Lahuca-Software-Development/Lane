package com.lahuca.lane.data.replicated;

import com.lahuca.lane.records.RecordConverter;

import java.util.Set;

/**
 * An object that can have subscribers with replicas.
 * This is applied on a replicated object, where it authoritatively lies.
 *
 * @param <T> The type of the ID
 * @param <R> The type of the record to sync
 */
public interface AuthoritativeObject<T, R extends Record> extends ReplicatedObject<T>, RecordConverter<R> {

    // TODO Do for dataobject, players, etc.

    /**
     * The subscribers of this object that have replicas.
     *
     * @return The subscribers
     */
    Set<String> getReplicatedSubscribers();

    /**
     * Subscribes to this object.
     *
     * @param subscriber The ID of the subscriber.
     */
    void subscribeReplicated(String subscriber);

    /**
     * Unsubscribes from this object.
     *
     * @param subscriber The ID of the subscriber.
     */
    void unsubscribeReplicated(String subscriber);

}
