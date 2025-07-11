package com.lahuca.laneinstance.retrieval;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.connection.packet.PartyPacket;
import com.lahuca.lane.connection.request.Request;
import com.lahuca.lane.records.PartyRecord;
import com.lahuca.laneinstance.LaneInstance;

import java.util.HashSet;
import java.util.UUID;

/**
 * This object holds the retrieved data at a party.
 * It is implementation-dependent if the information within this object is real time on the controller.
 */
public class InstancePartyRetrieval implements Retrieval, LaneParty {

    private long retrievalTimestamp;

    private long partyId;
    private UUID owner;
    private HashSet<UUID> players;
    private boolean invitationsOnly = true;
    private long creationTimestamp;

    public InstancePartyRetrieval(PartyRecord record) {
        this.retrievalTimestamp = System.currentTimeMillis();
        this.partyId = record.partyId();
        this.owner = record.owner();
        this.players = record.players();
        this.creationTimestamp = record.creationTimestamp();
    }

    @Override
    public long getRetrievalTimestamp() {
        return retrievalTimestamp;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    @Override
    public long getId() {
        return partyId;
    }

    @Override
    public HashSet<UUID> getPlayers() {
        return players;
    }

    public boolean isInvitationsOnly() {
        return invitationsOnly;
    }

    /**
     * Sets whether this party only allows new players by using invitations.
     * When the request is successful, the value in this object is changed.
     *
     * @param invitationsOnly if new players need to be invited
     * @return a request that is completed when it is successfully updated
     */
    public Request<Void> setInvitationsOnly(boolean invitationsOnly) {
        return LaneInstance.getInstance().getConnection().<Void>sendRequestPacket(id -> new PartyPacket.SetInvitationsOnly(id, partyId, invitationsOnly), null)
                .thenApplyConstruct(success -> {
                    InstancePartyRetrieval.this.invitationsOnly = invitationsOnly;
                    return null;
                });
    }

    /**
     * Returns whether the given player is invited to the party of this retrieval.
     *
     * @param player the uuid of the player to check
     * @return a request that is completed with the result: {@code true} if the player is invited, otherwise {@code false}
     * @throws IllegalArgumentException if the argument is null
     */
    public Request<Boolean> hasInvitation(UUID player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        return LaneInstance.getInstance().getConnection().sendRequestPacket(id -> new PartyPacket.Invitation.Has(id, partyId, player), null);
    }

    /**
     * Invites the given player to the party of this retrieval.
     *
     * @param player the uuid of the player to invite
     * @return a request that is completed with the result: {@code true} if the player was invited, otherwise {@code false}
     * @throws IllegalArgumentException if the argument is null
     */
    public Request<Boolean> addInvitation(UUID player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        return LaneInstance.getInstance().getConnection().sendRequestPacket(id -> new PartyPacket.Invitation.Add(id, partyId, player), null);
    }

    /**
     * Accepts the invitation of the given player from the party of this retrieval.
     * This only works when the party is in invitation-only mode, and they have received an invitation.
     * The given player should not be in another party.
     * When the request is successful, the player is added to this object.
     *
     * @param player the uuid of the player
     * @return a request that is completed with the result: {@code true} if the player is now in the party, otherwise {@code false}
     * @throws IllegalArgumentException if the argument is null
     */
    public Request<Boolean> acceptInvitation(UUID player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.Invitation.Accept(id, partyId, player), null)
                .thenApplyConstruct(status -> {
                    if (status) {
                        players.add(player);
                    }
                    return status;
                });
    }

    /**
     * Denies the invitation of the given player from the party of this retrieval.
     * This only works when the party is in invitation-only mode, and they have received an invitation.
     *
     * @param player the uuid of the player
     * @return a request that is completed with the result: {@code true} if the invitation has been removed, otherwise {@code false}
     * @throws IllegalArgumentException if the argument is null
     */
    public Request<Boolean> denyInvitation(UUID player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        return LaneInstance.getInstance().getConnection().sendRequestPacket(id -> new PartyPacket.Invitation.Add(id, partyId, player), null);
    }

    /**
     * Joins the party of this retrieval for the given player.
     * This only works when the party is not in invitation-only mode, i.e., everyone can join freely.
     * The given player should not be in another party.
     * When the request is successful, the player is added to this object.
     *
     * @param player the uuid of the player
     * @return a request that is completed with the result: {@code true} if the player is now in the party, otherwise {@code false}
     * @throws IllegalArgumentException if the argument is null
     */
    public Request<Boolean> joinPlayer(UUID player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.JoinPlayer(id, partyId, player), null)
                .thenApplyConstruct(status -> {
                    if (status) {
                        players.add(player);
                    }
                    return status;
                });
    }

    /**
     * Removes the player from the party of this retrieval.
     * When the request is successful, the player is removed from this object.
     *
     * @param player the uuid of the player
     * @return a request that is completed with the result: {@code true} if the player has been removed from the party, otherwise {@code false}
     * @throws IllegalArgumentException if the argument is null
     */
    public Request<Boolean> removePlayer(UUID player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.RemovePlayer(id, partyId, player), null)
                .thenApplyConstruct(status -> {
                    if (status) {
                        players.remove(player);
                    }
                    return status;
                });
    }

    /**
     * Disbands the party of this retrieval: removes this party object and sets the party of all players to null.
     * When the request is successful, all players are removed from this object. This object should not be used any more.
     *
     * @return a request that is completed with the result: {@code true} when the party has been removed, otherwise {@code false}
     */
    public Request<Boolean> disband() {
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.Disband(id, partyId), null)
                .thenApplyConstruct(status -> {
                    if (status) {
                        players.clear();
                    }
                    return status;
                });
    }

    /**
     * Changes the owner of the party from this retrieval to the given player.
     * When the request is successful, the owner is changed of this object.
     *
     * @param player the uuid of the player
     * @return a request that is completed with the result: {@code true} if the player is now the owner of the party (or was already the owner), otherwise {@code false}
     * @throws IllegalArgumentException if the argument is null
     */
    public Request<Boolean> setOwner(UUID player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        return LaneInstance.getInstance().getConnection().<Boolean>sendRequestPacket(id -> new PartyPacket.SetOwner(id, partyId, player), null)
                .thenApplyConstruct(status -> {
                    if (status) {
                        owner = player;
                    }
                    return status;
                });
    }

    /**
     * Warps all party members of the party identified by this retrieval to the owner's game/instance.
     *
     * @return a request that is completed with the result: {@code true} whether the party has been warped, otherwise {@code false}
     */
    public Request<Boolean> warpParty() {
        return LaneInstance.getInstance().getConnection().sendRequestPacket(id -> new PartyPacket.Warp(id, partyId), null);
    }

}
