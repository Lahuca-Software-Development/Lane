package com.lahuca.lanecontroller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lahuca.lane.LaneParty;
import com.lahuca.lane.connection.packet.PartyPacket;
import com.lahuca.lane.data.replicated.AuthoritativeObject;
import com.lahuca.lane.queue.QueueRequestParameter;
import com.lahuca.lane.queue.QueueRequestParameters;
import com.lahuca.lane.records.PartyRecord;
import com.lahuca.lanecontroller.events.party.*;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class ControllerParty implements LaneParty, AuthoritativeObject<Long, PartyRecord> {

    // TODO Do we maybe want to add roles to party members? This could for example allow other players to kick players instead of only the owner.

    private final long partyId;
    private UUID owner;
    private final HashSet<UUID> players = new HashSet<>();
    private boolean invitationsOnly = true;

    private final Cache<@NotNull UUID, String> invitations;
    private final long creationTimestamp;

    private final HashSet<String> replicatedSubscribers = new HashSet<>();

    ControllerParty(long partyId, ControllerPlayer owner) {
        Objects.requireNonNull(owner, "owner is null");
        if (owner.getParty().isPresent()) throw new IllegalStateException("owner is already in a party");
        this.partyId = partyId;
        invitations = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES)
                .removalListener((key, value, cause) -> clearSoloParty()).build(); // TODO Expire event and packet?
        creationTimestamp = System.currentTimeMillis();
        players.add(owner.getUuid());
        // TODO we cannot do owner.setPartyId here due to the object not being registered yet
        setOwner(owner);
    }

    @Override
    public long getId() {
        return partyId;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    public ControllerPlayer getControllerOwner() {
        return Controller.getPlayer(owner).orElse(null); // TODO We are not sure if they are online
    }

    @Override
    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    /**
     * Returns the party's players as a {@link HashSet} ofo {@link ControllerPlayer} objects.
     *
     * @return the controller players in this party
     */
    public HashSet<ControllerPlayer> getControllerPlayers() {
        HashSet<ControllerPlayer> controllerPlayers = new HashSet<>();
        players.forEach(uuid -> Controller.getPlayer(uuid).ifPresent(controllerPlayers::add));
        return controllerPlayers; // TODO We are not sure if all of them are online. Rename function maybe?
    }

    /**
     * Returns whether the player is part of this party.
     *
     * @param player the player
     * @return {@code true} if the player is part of the party
     * @throws IllegalArgumentException if {@code player} is null
     */
    public boolean containsPlayer(ControllerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        return getPlayers().contains(player.getUuid());
    }

    @Override
    public boolean isInvitationsOnly() {
        return invitationsOnly;
    }

    /**
     * Sets whether this party only allows new players by using invitations.
     *
     * @param invitationsOnly if new players need to be invited
     */
    public void setInvitationsOnly(boolean invitationsOnly) {
        boolean old = this.invitationsOnly;
        this.invitationsOnly = invitationsOnly;
        if(old != invitationsOnly) {
            Controller.getInstance().handleControllerEvent(new PartySetInvitationsOnlyEvent(this, invitationsOnly));
            Controller.getInstance().getConnection().sendPacket(getReplicatedSubscribers(), new PartyPacket.Event.SetInvitationsOnly(partyId, invitationsOnly, convertRecord()));
        }
    }

    @Override
    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    @Override
    public Set<UUID> getUnmodifiableInvitations() {
        return Collections.unmodifiableSet(invitations.asMap().keySet());
    }

    @Override
    public PartyRecord convertRecord() {
        return new PartyRecord(partyId, owner, players, invitationsOnly, creationTimestamp, getUnmodifiableInvitations());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ControllerParty.class.getSimpleName() + "[", "]")
                .add("partyId=" + partyId)
                .add("owner=" + owner)
                .add("players=" + players)
                .add("invitationsOnly=" + invitationsOnly)
                .add("invitations=" + invitations)
                .add("creationTimestamp=" + creationTimestamp)
                .add("replicatedSubscribers=" + replicatedSubscribers)
                .toString();
    }

    @Override
    public HashSet<String> getReplicatedSubscribers() {
        HashSet<String> subscribers = new HashSet<>(replicatedSubscribers);
        // Next to the subscribed instances, there are additional subscribers: the different instances of the party members
        getPlayers().forEach(uuid -> Controller.getPlayer(uuid).flatMap(ControllerPlayer::getInstanceId).ifPresent(subscribers::add));
        return subscribers;
    }

    @Override
    public void subscribeReplicated(String subscriber) {
        replicatedSubscribers.add(subscriber);
    }

    @Override
    public void unsubscribeReplicated(String subscriber) {
        replicatedSubscribers.remove(subscriber);
    }

    public Cache<@NotNull UUID, String> getInvitations() {
        return invitations;
    }

    /**
     * Returns whether the given player is invited to this party.
     *
     * @param player the player
     * @return {@code true} if the player is invited, otherwise {@code false}
     * @throws IllegalArgumentException if {@code player} is null
     */
    public boolean hasInvitation(ControllerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        return invitations.getIfPresent(player.getUuid()) != null;
    }

    /**
     * Invites the given player to this party.
     * This only works when the party is in invitation-only mode, and they have not yet received an invitation to this party.
     *
     * @param player the player
     * @return {@code true} if the player was invited, otherwise {@code false}
     * @throws IllegalArgumentException if {@code player} is null
     */
    public boolean addInvitation(ControllerPlayer player) {
        if (player == null)
            throw new IllegalArgumentException("player is null"); // TODO Check this in the whole codebase!
        if (!isInvitationsOnly() || hasInvitation(player) || containsPlayer(player)) return false;
        invitations.put(player.getUuid(), player.getUsername());
        Controller.getInstance().handleControllerEvent(new PartyAddInvitationEvent(this, player));
        Controller.getInstance().getConnection().sendPacket(getReplicatedSubscribers(), new PartyPacket.Event.AddInvitation(partyId, player.getUuid(), convertRecord()));
        return true;
    }

    /**
     * Accepts the invitation of the given player.
     * This only works when the party is in invitation-only mode, and they have received an invitation.
     * The given player should not be in another party.
     *
     * @param player the player
     * @return {@code true} if the player is now in the party, otherwise {@code false}
     * @throws IllegalArgumentException if {@code player} is null
     */
    public boolean acceptInvitation(ControllerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        if (!isInvitationsOnly() || !hasInvitation(player) || containsPlayer(player)) return false;
        if (player.getParty().isPresent()) return false;
        invitations.invalidate(player.getUuid());
        players.add(player.getUuid());
        player.setPartyId(partyId);
        Controller.getInstance().handleControllerEvent(new PartyAcceptInvitationEvent(this, player));
        Controller.getInstance().getConnection().sendPacket(getReplicatedSubscribers(), new PartyPacket.Event.AcceptInvitation(partyId, player.getUuid(), convertRecord()));
        return true;
    }

    /**
     * Denies the invitation of the given player.
     * This only works when the party is in invitation-only mode, and they have received an invitation.
     *
     * @param player the player
     * @return {@code true} if the invitation has been removed, otherwise {@code false}
     * @throws IllegalArgumentException if {@code player} is null
     */
    public boolean denyInvitation(ControllerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        if (!isInvitationsOnly() || !hasInvitation(player) || containsPlayer(player)) return false;
        if (invitations.getIfPresent(player.getUuid()) == null) return false;
        invitations.invalidate(player.getUuid());
        Controller.getInstance().handleControllerEvent(new PartyDenyInvitationEvent(this, player));
        Controller.getInstance().getConnection().sendPacket(getReplicatedSubscribers(), new PartyPacket.Event.DenyInvitation(partyId, player.getUuid(), convertRecord()));
        return true;
    }

    /**
     * Joins the party for the given player.
     * This only works when the party is not in invitation-only mode, i.e., everyone can join freely.
     * The given player should not be in another party.
     *
     * @param player the player
     * @return {@code true} if the player is now in the party, otherwise {@code false}
     * @throws IllegalArgumentException if {@code player} is null
     */
    public boolean joinPlayer(ControllerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        if (isInvitationsOnly() || containsPlayer(player)) return false;
        if (player.getParty().isPresent()) return false;
        players.add(player.getUuid());
        player.setPartyId(partyId);
        invitations.invalidate(player.getUuid());
        Controller.getInstance().handleControllerEvent(new PartyJoinPlayerEvent(this, player));
        Controller.getInstance().getConnection().sendPacket(getReplicatedSubscribers(), new PartyPacket.Event.JoinPlayer(partyId, player.getUuid(), convertRecord()));
        return true;
    }

    /**
     * Removes the player from the party.
     *
     * @param player the player
     * @return {@code true} if the player has been removed from the party, otherwise {@code false}
     * @throws IllegalArgumentException if {@code player} is null
     */
    public boolean removePlayer(ControllerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        if (!containsPlayer(player)) return false;
        invitations.invalidate(player.getUuid());
        if (getOwner().equals(player.getUuid())) {
            if (disband()) {
                Component message = Component.translatable("lane.controller.party.ownerLeave"); // TODO Add setting to not send message?
                getPlayers().forEach(uuid -> Controller.getInstance().sendMessage(uuid, message));
                return true;
            }
            return false;
        }
        players.remove(player.getUuid());
        player.setPartyId(null);
        if(!clearSoloParty()) {
            Controller.getInstance().handleControllerEvent(new PartyRemovePlayerEvent(this, player));
            Controller.getInstance().getConnection().sendPacket(getReplicatedSubscribers(), new PartyPacket.Event.RemovePlayer(partyId, player.getUuid(), convertRecord()));
        }
        return true;
    }

    public boolean clearSoloParty() {
        if (isSoloParty() && isInvitationsOnly() && getInvitations().estimatedSize() == 0) {
            // Private and only one person left
            if (disband()) {
                Component message = Component.translatable("lane.controller.party.clearSoloParty"); // TODO Add setting to not send message?
                getPlayers().forEach(uuid -> Controller.getInstance().sendMessage(uuid, message));
                return true;
            }
        }
        return false;
    }

    /**
     * Delegation of the method in the controller {@link ControllerPartyManager#disbandParty(ControllerParty)},
     * due to complete removal of the object.
     * Disbands this party: removes this party object and sets the party of all players to null.
     *
     * @return {@code true} when the party has been removed, {@code false} otherwise
     * @see ControllerPartyManager#disbandParty(ControllerParty)
     */
    public boolean disband() {
        return Controller.getInstance().getPartyManager().disbandParty(this);
    }

    /**
     * Changes the owner to the given player.
     * This only works if the given player is already in the party.
     *
     * @param player the player
     * @return {@code true} if the player is now the owner of the party (or was already the owner), otherwise {@code false}
     * @throws IllegalArgumentException if {@code player} is null
     */
    public boolean setOwner(ControllerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player is null");
        if (!containsPlayer(player)) return false; // TODO Also check on the player itself?
        owner = player.getUuid();
        Controller.getInstance().handleControllerEvent(new PartySetOwnerEvent(this, player));
        Controller.getInstance().getConnection().sendPacket(getReplicatedSubscribers(), new PartyPacket.Event.SetOwner(partyId, player.getUuid(), convertRecord()));
        return true;
    }

    /**
     * Warps all party members to the owner's game/instance.
     * This is different from when the owner joins a game, as a party join is automatically applied in that case
     *
     * @return {@code true} whether the party has been warped, {@code false} otherwise
     */
    public boolean warpParty() {
        Optional<ControllerPlayer> ownerOpt = Controller.getInstance().getPlayerManager().getPlayer(getOwner());
        if (ownerOpt.isEmpty()) return false;
        ControllerPlayer player = ownerOpt.get();
        QueueRequestParameters queue;
        if (player.getGameId().isPresent()) {
            queue = QueueRequestParameter.create().gameId(player.getGameId().get()).partySkip(false).buildParameters();
        } else if (player.getInstanceId().isPresent()) {
            queue = QueueRequestParameter.create().instanceId(player.getInstanceId().get()).partySkip(false).buildParameters();
        } else {
            return false;
        }
        getControllerPlayers().forEach(current -> {
            if (!current.getUuid().equals(player.getUuid())) {
                current.queue(queue); // TODO What if any of them fail?
            }
        });
        Controller.getInstance().handleControllerEvent(new PartyWarpEvent(this));
        Controller.getInstance().getConnection().sendPacket(getReplicatedSubscribers(), new PartyPacket.Event.Warp(partyId));
        return true;
    }

}
