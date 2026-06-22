/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 12:38 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane;

import com.lahuca.lane.data.replicated.ReplicatedObject;

import java.util.Set;
import java.util.UUID;

public interface LaneParty extends LaneRelationship, ReplicatedObject<Long> {

    @Override
    default Long getReplicationId() {
        return getId();
    }

    /**
     * Gets owner of this party.
     *
     * @return The owner's uuid of this party
     */
    UUID getOwner();

    /**
     * Returns whether this party is public or not.
     *
     * @return {@code false} if the party is public, otherwise {@code true}
     */
    boolean isInvitationsOnly();

    /**
     * Returns the player limit of this party.
     * The limit determines how many players can join this party.
     *
     * @return the player limit (positive, greater than two), null if no limit is present
     */
    Integer getPlayerLimit();

    /**
     * Gets a time stamp when this party was created
     *
     * @return The time stamp where was this party created
     */
    long getCreationTimestamp();

    /**
     * Returns whether the party contains only one player.
     * This happens when the party is public or if it as outgoing invitations.
     *
     * @return {@code true} if the party is solo, otherwise {@code false}
     */
    default boolean isSoloParty() {
        return getPlayers().size() == 1;
    }

    /**
     * Retrieves the set of outgoing invitations.
     * This set is unmodifiable due to the high caching requirements on it.
     *
     * @return the set
     */
    Set<UUID> getUnmodifiableInvitations();

    /**
     * Returns whether the party is full.
     * If the party has a player limit, this method returns whether the party is full.
     * @return {@code true} if the party is full, otherwise {@code false}
     */
    default boolean isFull() {
        return getPlayerLimit() != null && getPlayers().size() >= getPlayerLimit();
    }

}
