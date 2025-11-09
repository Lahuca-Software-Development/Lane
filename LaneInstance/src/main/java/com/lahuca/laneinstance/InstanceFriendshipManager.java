package com.lahuca.laneinstance;

import com.lahuca.lane.FriendshipInvitation;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.packet.FriendshipPacket;
import com.lahuca.lane.records.RelationshipRecord;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InstanceFriendshipManager {

    private final LaneInstance instance;

    public InstanceFriendshipManager(LaneInstance instance) {
        this.instance = instance;
    }

    public String id() {
        return instance.getId();
    }

    public Connection connection() {
        return instance.getConnection();
    }

    // TODO Packet side of controller has been handled, only functions here need to be made
    // TODO Maybee do input checking before sending?

    /**
     * Retrieves all invitations
     *
     * @return a {@link CompletableFuture} with a map of invitations
     */
    public CompletableFuture<List<FriendshipInvitation>> getInvitations() {
        return connection().<List<FriendshipInvitation>>sendRequestPacket(id ->
                new FriendshipPacket.GetInvitations(id, null, null,  null), null).getResult();
    }

    /**
     * Retrieves all invitations for the player's network profile.
     *
     * @param player           the player
     * @param includeRequester if the player should be the requester
     * @param includeInvited   if the player should be invited
     * @return the map
     */
    public CompletableFuture<List<FriendshipInvitation>> getInvitations(InstancePlayer player, boolean includeRequester, boolean includeInvited) {
        return connection().<List<FriendshipInvitation>>sendRequestPacket(id ->
                new FriendshipPacket.GetInvitations(id, player.getUuid(), includeRequester, includeInvited), null).getResult();
    }

    /**
     * Returns the username of the requester, or null if the invitation is not present.
     *
     * @param invitation the invitation
     * @return a {@link CompletableFuture} with the name of the requester if present, null otherwise
     */
    public CompletableFuture<String> containsInvitation(FriendshipInvitation invitation) {
        return connection().<String>sendRequestPacket(id -> new FriendshipPacket.ContainsInvitation(id, invitation), null).getResult();
    }

    /**
     * Invalidates the invitation.
     *
     * @param invitation the invitation
     * @return a {@link CompletableFuture} that completes once the invitation has been invalidated.
     */
    public CompletableFuture<Void> invalidateInvitation(FriendshipInvitation invitation) {
        return connection().<Void>sendRequestPacket(id -> new FriendshipPacket.InvalidateInvitation(id, invitation), null).getResult();
    }

    /**
     * Invites a player for a friendship.
     *
     * @param invitation the invitation
     * @param username   the username of the requester
     * @return a {@link CompletableFuture} that completes once the invitation has been sent.
     */
    public CompletableFuture<Void> invite(FriendshipInvitation invitation, String username) {
        return connection().<Void>sendRequestPacket(id -> new FriendshipPacket.Invite(id, invitation, username), null).getResult();
    }

    /**
     * Accepts an invitation.
     *
     * @param invitation the invitation
     * @return a {@link CompletableFuture} that completes once the invitation has been accepted.
     */
    public CompletableFuture<Void> acceptInvitation(FriendshipInvitation invitation) {
        if (invitation.requester() == null || invitation.invited() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return connection().<Void>sendRequestPacket(id -> new FriendshipPacket.AcceptInvitation(id, invitation), null).getResult();
    }

    /**
     * Retrieves the friendship identified by the given ID.
     *
     * @param friendshipId the ID of the friendship
     * @return a {@link CompletableFuture} that completes with the friendship record, it is null when the friendship is not present.
     */
    public CompletableFuture<RelationshipRecord> getFriendship(long friendshipId) {
        return connection().<RelationshipRecord>sendRequestPacket(id -> new FriendshipPacket.GetFriendship(id, friendshipId), null).getResult();
    }

    /**
     * Fetches the friendships of the player:
     * <ol>
     *     <li>First fetches the friendship IDs: networkProfile.PROFILE_ID.friends.</li>
     *     <li>Then it combines the requests for the friendship data per ID: friends.ID.data.</li>
     *     <li>These requests are combined.
     *     <li>Then removes any of the friendship IDs from players.PLAYER.friends that are not present anymore.</li>
     *     <li>Then returns the friendships that are present.</li>
     * </ol>
     * Any errors caught during the operation are parsed in the CompletableFuture returned.
     *
     * @param player the player
     * @return a CompletableFuture that has a Collection with the friendships, the Collection is empty when the player has no friendships.
     */
    public CompletableFuture<Collection<RelationshipRecord>> getFriendships(InstancePlayer player) {
        // Fetch the friendship IDs
        return connection().<Collection<RelationshipRecord>>sendRequestPacket(id -> new FriendshipPacket.GetFriendships(id, player.getUuid()), null).getResult();
    }

    /**
     * Removes a friendship from the system.
     * This removes the friendship data first, then it removes the ID from both players
     *
     * @param friendshipId the friendship's ID to remove
     * @return a {@link CompletableFuture} that completes once the friendship has been removed.
     */
    public CompletableFuture<Void> removeFriendship(long friendshipId) {
        return connection().<Void>sendRequestPacket(id -> new FriendshipPacket.RemoveFriendship(id, friendshipId), null).getResult();
    }

}
