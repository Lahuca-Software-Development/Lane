package com.lahuca.laneinstance;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.connection.packet.PartyPacket;
import com.lahuca.lane.data.replicated.ReplicaObject;
import com.lahuca.lane.records.PartyRecord;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * This object holds the retrieved data at a party.
 * It is implementation-dependent of the information within this object is real time on the controller.
 */
public class InstanceParty implements LaneParty, ReplicaObject<Long, PartyRecord> {

    private long lastSyncReplicatedTime;
    private boolean subscribed;
    private boolean isRemovedReplicated = false;

    private long partyId;
    private UUID owner;
    private HashSet<UUID> players;
    private boolean invitationsOnly;
    private long creationTimestamp;
    private Set<UUID> unmodifiableInvitations;

    InstanceParty(PartyRecord record) {
        lastSyncReplicatedTime = System.currentTimeMillis();
        subscribed = true;
        subscribeReplicated();
        applyRecord(record);
    }

    @Override
    public long getLastSyncReplicatedTime() {
        return lastSyncReplicatedTime;
    }

    @Override
    public boolean isSubscribed() {
        return subscribed;
    }

    @Override
    public void subscribeReplicated() {
        LaneInstance.getInstance().getConnection().sendPacket(new PartyPacket.Retrieve.Subscribe(partyId), null);
        subscribed = true;
    }

    @Override
    public void unsubscribeReplicated() {
        LaneInstance.getInstance().getConnection().sendPacket(new PartyPacket.Retrieve.Unsubscribe(partyId), null);
        subscribed = false;
    }

    @Override
    public boolean isRemovedReplicated() {
        return isRemovedReplicated;
    }

    void removeReplicated() {
        isRemovedReplicated = true;
    }

    @Override
    public void applyRecord(PartyRecord record) {
        lastSyncReplicatedTime = System.currentTimeMillis();
        this.partyId = record.partyId();
        this.owner = record.owner();
        this.players = record.players();
        this.invitationsOnly = record.invitationsOnly();
        this.creationTimestamp = record.creationTimestamp();
        this.unmodifiableInvitations = record.unmodifiableInvitations();
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public long getId() {
        return partyId;
    }

    @Override
    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    @Override
    public boolean isInvitationsOnly() {
        return invitationsOnly;
    }

    @Override
    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    @Override
    public Set<UUID> getUnmodifiableInvitations() {
        return unmodifiableInvitations;
    }

    /**
     * Sets whether this party only allows new players by using invitations.
     * When the request is successful, the value in this object is changed.
     *
     * @param invitationsOnly if new players need to be invited
     * @return a {@link CompletableFuture} that is completed when it is successfully updated
     */
    public CompletableFuture<Void> setInvitationsOnly(boolean invitationsOnly) {
        return LaneInstance.getInstance().getConnection().<Void>sendRequestPacket(id -> new PartyPacket.Operations.SetInvitationsOnly(id, partyId, invitationsOnly), null).getResult();
    }

    /**
     * Invites the given player to the party.
     *
     * @param player the uuid of the player to invite
     * @return a {@link CompletableFuture} that is completed with the result: {@code true} if the player was invited, otherwise {@code false}
     */
    public CompletableFuture<Boolean> addInvitation(UUID player) {
        Objects.requireNonNull(player, "player is null");
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.Operations.AddInvitation(id, partyId, player), null).getResult();
    }

    /**
     * Accepts the invitation of the given player from the party.
     * This only works when the party is in invitation-only mode, and they have received an invitation.
     * The given player should not be in another party.
     * When the request is successful, the player is added to this object.
     *
     * @param player the uuid of the player
     * @return a {@link CompletableFuture} that is completed with the result: {@code true} if the player is now in the party, otherwise {@code false}
     */
    public CompletableFuture<Boolean> acceptInvitation(UUID player) {
        Objects.requireNonNull(player, "player is null");
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.Operations.AcceptInvitation(id, partyId, player), null).getResult();
    }

    /**
     * Denies the invitation of the given player from the party.
     * This only works when the party is in invitation-only mode, and they have received an invitation.
     *
     * @param player the uuid of the player
     * @return a {@link CompletableFuture} that is completed with the result: {@code true} if the invitation has been removed, otherwise {@code false}
     */
    public CompletableFuture<Boolean> denyInvitation(UUID player) {
        Objects.requireNonNull(player, "player is null");
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.Operations.DenyInvitation(id, partyId, player), null).getResult();
    }

    /**
     * Joins the party for the given player.
     * This only works when the party is not in invitation-only mode, i.e. everyone can join freely.
     * The given player should not be in another party.
     * When the request is successful, the player is added to this object.
     *
     * @param player the uuid of the player
     * @return a {@link CompletableFuture} that is completed with the result: {@code true} if the player is now in the party, otherwise {@code false}
     */
    public CompletableFuture<Boolean> joinPlayer(UUID player) {
        Objects.requireNonNull(player, "player is null");
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.Operations.JoinPlayer(id, partyId, player), null).getResult();
    }

    /**
     * Removes the player from the party.
     * When the request is successful, the player is removed from this object.
     *
     * @param player the uuid of the player
     * @return a {@link CompletableFuture} that is completed with the result: {@code true} if the player has been removed from the party, otherwise {@code false}
     */
    public CompletableFuture<Boolean> removePlayer(UUID player) {
        Objects.requireNonNull(player, "player is null");
        if (player == null) throw new IllegalArgumentException("player is null");
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.Operations.RemovePlayer(id, partyId, player), null).getResult();
    }

    /**
     * Disbands the party: removes this party object and sets the party of all players to null.
     * When the request is successful, all players are removed from this object. This object should not be used any more.
     *
     * @return a {@link CompletableFuture} that is completed with the result: {@code true} when the party has been removed, otherwise {@code false}
     */
    public CompletableFuture<Boolean> disband() {
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.Operations.Disband(id, partyId), null).getResult();
    }

    /**
     * Changes the owner of the party to the given player.
     * When the request is successful, the owner is changed of this object.
     *
     * @param player the uuid of the player
     * @return a {@link CompletableFuture} that is completed with the result: {@code true} if the player is now the owner of the party (or was already the owner), otherwise {@code false}
     */
    public CompletableFuture<Boolean> setOwner(UUID player) {
        Objects.requireNonNull(player, "player is null");
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.Operations.SetOwner(id, partyId, player), null).getResult();
    }

    /**
     * Warps all party members of the party to the owner's game/instance.
     *
     * @return a {@link CompletableFuture} that is completed with the result: {@code true} whether the party has been warped, otherwise {@code false}
     */
    public CompletableFuture<Boolean> warpParty() {
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.Operations.Warp(id, partyId), null).getResult();
    }

}
