package com.lahuca.lanecontroller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lahuca.lane.LaneParty;
import com.lahuca.lane.queue.QueueRequestParameter;
import com.lahuca.lane.queue.QueueRequestParameters;
import com.lahuca.lane.records.PartyRecord;

import java.util.HashSet;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class ControllerParty implements LaneParty {

    // TODO Do we maybe want to add roles to party members? This could for example allow other players to kick players instead of only the owner.

    private final long partyId;
    private UUID owner;
    private final HashSet<UUID> players = new HashSet<>();
    private boolean invitationsOnly = true;

    private final Cache<UUID, String> invitations;
    private final long creationTimestamp;

    ControllerParty(long partyId, UUID owner) { // TODO Maybe not public. VERY IMPORTANT!!!
        this.partyId = partyId;
        setOwner(owner);

        invitations = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
        creationTimestamp = System.currentTimeMillis();
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
    public HashSet<UUID> getPlayers() {
        return players; // TODO Maybe immutable?
    }

    /**
     * Returns the party's players as a {@link HashSet} ofo {@link ControllerPlayer} objects.
     * @return the controller players in this party
     */
    public HashSet<ControllerPlayer> getControllerPlayers() {
        HashSet<ControllerPlayer> controllerPlayers = new HashSet<>();
        players.forEach(uuid -> Controller.getPlayer(uuid).ifPresent(controllerPlayers::add));
        return controllerPlayers; // TODO We are not sure if all of them are online. Rename function maybe?
    }

    /**
     * Returns whether the player with the given UUID is part of this party.
     *
     * @param uuid the uuid of the player
     * @return {@code true} if the player is part of the party
     * @throws IllegalArgumentException if {@code uuid} is null
     */
    public boolean containsPlayer(UUID uuid) {
        if (uuid == null) throw new IllegalArgumentException("uuid is null");
        return getPlayers().contains(uuid);
    }

    public boolean isInvitationsOnly() {
        return invitationsOnly;
    }

    /**
     * Sets whether this party only allows new players by using invitations.
     *
     * @param invitationsOnly if new players need to be invited
     */
    public void setInvitationsOnly(boolean invitationsOnly) {
        this.invitationsOnly = invitationsOnly;
    }

    @Override
    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public PartyRecord convertToRecord() {
        return new PartyRecord(owner, players, creationTimestamp);
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
                .toString();
    }

    public Cache<UUID, String> getInvitations() {
        return invitations;
    }

    // TODO Invite first, then do accept, deny, etc.. Remove addPlayer(ControllerPlayer);...

    /**
     * Returns whether the given player is invited to this party.
     *
     * @param uuid the uuid of the player
     * @return {@code true} if the player is invited, otherwise {@code false}
     * @throws IllegalArgumentException if {@code uuid} is null
     */
    public boolean hasInvitation(UUID uuid) {
        if (uuid == null) throw new IllegalArgumentException("uuid is null");
        return invitations.getIfPresent(uuid) != null;
    }

    /**
     * Invites the given player to this party.
     * This only works when the party is in invitation-only mode, and they have not yet received an invitation to this party.
     *
     * @param uuid the player to invite
     * @return {@code true} if the player was invited, otherwise {@code false}
     * @throws IllegalArgumentException if {@code uuid} is null
     */
    public boolean addInvitation(UUID uuid) {
        if (uuid == null) throw new IllegalArgumentException("uuid is null"); // TODO Check this in the whole codebase!
        if (!isInvitationsOnly() || hasInvitation(uuid) || containsPlayer(uuid)) return false;
        Optional<ControllerPlayer> playerOptional = Controller.getPlayer(uuid);
        if (playerOptional.isEmpty()) return false;
        playerOptional.ifPresent(player -> invitations.put(uuid, player.getDisplayName()));
        return true;
    }

    /**
     * Accepts the invitation of the given player.
     * This only works when the party is in invitation-only mode, and they have received an invitation.
     * The given player should not be in another party.
     *
     * @param uuid the uuid of the player
     * @return {@code true} if the player is now in the party, otherwise {@code false}
     * @throws IllegalArgumentException if {@code uuid} is null
     */
    public boolean acceptInvitation(UUID uuid) {
        if (uuid == null) throw new IllegalArgumentException("uuid is null");
        if (!isInvitationsOnly() || !hasInvitation(uuid) || containsPlayer(uuid)) return false;
        Optional<ControllerPlayer> playerOptional = Controller.getPlayer(uuid);
        if (playerOptional.isEmpty()) return false;
        ControllerPlayer player = playerOptional.get();
        if (player.getParty().isPresent()) return false;
        invitations.invalidate(uuid);
        players.add(uuid);
        player.setPartyId(partyId);
        return true;
    }

    /**
     * Denies the invitation of the given player.
     * This only works when the party is in invitation-only mode, and they have received an invitation.
     *
     * @param uuid the uuid of the player
     * @return {@code true} if the invitation has been removed, otherwise {@code false}
     */
    public boolean denyInvitation(UUID uuid) {
        if (uuid == null) throw new IllegalArgumentException("uuid is null");
        if (!isInvitationsOnly() || !hasInvitation(uuid) || containsPlayer(uuid)) return false;
        if (invitations.getIfPresent(uuid) == null) return false;
        invitations.invalidate(uuid);
        return true;
    }

    /**
     * Joins the party for the given player.
     * This only works when the party is not in invitation-only mode, i.e., everyone can join freely.
     * The given player should not be in another party.
     * @param uuid the uuid of the player
     * @return {@code true} if the player is now in the party, otherwise {@code false}
     * @throws IllegalArgumentException if {@code uuid} is null
     */
    public boolean joinPlayer(UUID uuid) {
        if (uuid == null) throw new IllegalArgumentException("uuid is null");
        if (isInvitationsOnly() || containsPlayer(uuid)) return false;
        Optional<ControllerPlayer> playerOptional = Controller.getPlayer(uuid);
        if (playerOptional.isEmpty()) return false;
        ControllerPlayer player = playerOptional.get();
        if (player.getParty().isPresent()) return false;
        players.add(uuid);
        player.setPartyId(partyId);
        invitations.invalidate(uuid);
        return true;
    }

    /**
     * Removes the player from the party.
     * @param uuid the uuid of the player
     * @return {@code true} if the player has been removed from the party, otherwise {@code false}
     * @throws IllegalArgumentException if {@code uuid} is null
     */
    public boolean removePlayer(UUID uuid) {
        if (uuid == null) throw new IllegalArgumentException("uuid is null");
        if (!containsPlayer(uuid)) return false;
        Optional<ControllerPlayer> playerOptional = Controller.getPlayer(uuid);
        if (playerOptional.isEmpty()) return false;
        invitations.invalidate(uuid);
        players.remove(uuid);
        playerOptional.get().setPartyId(null);
        return true;
    }

    /**
     * Delegation of the method in the controller {@link ControllerPartyManager#disbandParty(ControllerParty)},
     * due to complete removal of the object.
     * Disbands this party: removes this party object and sets the party of all players to null.
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
     * @param uuid the uuid of the player
     * @return {@code true} if the player is now the owner of the party (or was already the owner), otherwise {@code false}
     * @throws IllegalArgumentException if {@code uuid} is null
     */
    public boolean setOwner(UUID uuid) { // TODO Maybe use ControllerPlayer, to make it more nice?
        if (uuid == null) throw new IllegalArgumentException("uuid is null");
        if (!containsPlayer(uuid)) return false; // TODO Also check on the player itself?
        owner = uuid;
        return true;
    }

    /**
     * Warps all party members to the owner's game/instance.
     * This is different then when the owner joins a game, as a party join is automatically applied in that case
     * @return {@code true} whether the party has been warped, {@code false} otherwise
     */
    public boolean warpParty() {
        Optional<ControllerPlayer> ownerOpt = Controller.getInstance().getPlayerManager().getPlayer(getOwner());
        if(ownerOpt.isEmpty()) return false;
        ControllerPlayer player = ownerOpt.get();
        QueueRequestParameters queue;
        if(player.getGameId().isPresent()) {
            queue = QueueRequestParameter.create().gameId(player.getGameId().get()).buildParameters();
        } else if(player.getInstanceId().isPresent()) {
            queue = QueueRequestParameter.create().instanceId(player.getInstanceId().get()).buildParameters();
        } else {
            return false;
        }
        getControllerPlayers().forEach(current -> {
            if(!current.getUuid().equals(player.getUuid())) {
                current.queue(queue); // TODO What if any of them fail?
            }
        });
        return true;
    }



}
