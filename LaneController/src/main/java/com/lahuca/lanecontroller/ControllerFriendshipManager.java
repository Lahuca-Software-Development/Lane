package com.lahuca.lanecontroller;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.RelationalId;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.records.RecordConverter;
import com.lahuca.lane.records.RelationshipRecord;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ControllerFriendshipManager {

    private final Controller controller;
    private final DataManager dataManager;
    private final Gson gson;

    /**
     * A cache for saving the friendship IDs to a network profile.
     */
    private final AsyncLoadingCache<@NotNull UUID, List<Long>> friendshipIds;
    /**
     * A cache for saving the friendship information identified by the ID of a friendship.
     */
    private final AsyncLoadingCache<@NotNull Long, RelationshipRecord> friendships;
    /**
     * A cache for saving the friend invites.
     * The key represents a combination of the two UUIDs (network profile), the value is the username of the requester.
     */
    private final Cache<@NotNull FriendshipInvitation, String> invitations;

    public ControllerFriendshipManager(Controller controller, DataManager dataManager, Gson gson) {
        this.controller = controller;
        this.dataManager = dataManager;
        this.gson = gson;
        // TODO Probably change expiration times!
        friendshipIds = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).buildAsync((uuid, executor) ->
                DefaultDataObjects.getNetworkProfilesFriends(dataManager, gson, uuid).thenApply(opt -> opt.orElse(null)));
        friendships = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).buildAsync((uuid, executor) ->
                DefaultDataObjects.getFriendshipsData(dataManager, gson, uuid).thenApply(opt -> opt.orElse(null)));
        invitations = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
    }

    /**
     * Retrieves all invitations
     *
     * @return the map
     */
    public ConcurrentMap<FriendshipInvitation, String> getInvitations() {
        return invitations.asMap();
    }

    /**
     * Returns the username of the requester, or null if the invitation is not present.
     * @param invitation the invitation
     * @return the name
     */
    public String containsInvitation(FriendshipInvitation invitation) {
        return invitations.getIfPresent(invitation);
    }

    /**
     * Retrieves all invitations for the player's network profile.
     *
     * @param player           the player's UUID
     * @param includeRequester if the player should be the requester
     * @param includeInvited   if the player should be invited
     * @return the map
     */
    public ConcurrentMap<FriendshipInvitation, String> getInvitations(ControllerPlayer player, boolean includeRequester, boolean includeInvited) {
        return invitations.asMap().entrySet().parallelStream()
                .filter(entry -> {
                    FriendshipInvitation pair = entry.getKey();
                    return (includeRequester && pair.requester().equals(player.getNetworkProfileUuid()))
                            || (includeInvited && pair.invited().equals(player.getNetworkProfileUuid()));
                }).collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void invalidateInvitation(FriendshipInvitation invitation) {
        invitations.invalidate(invitation);
    }

    public void invite(FriendshipInvitation invitation, String username) {
        invitations.put(invitation, username); // TODO Additional things? checks?
    }

    public CompletableFuture<Void> acceptInvitation(FriendshipInvitation invitation) {
        if (invitation.requester == null || invitation.invited == null) {
            return CompletableFuture.completedFuture(null);
        }
        return newId().thenCompose(id -> {
            // We got a new friendship ID
            // Write friendship data first
            return DefaultDataObjects.setFriendshipsData(dataManager, gson, id, invitation.convertRecord()).thenCompose(status -> {
                // Written, yeey, now for both players add to the friendships
                // Remove from data
                // TODO What if failed?
                return DefaultDataObjects.addNetworkProfilesFriends(dataManager, gson, invitation.requester, id)
                        .thenCompose(status2 -> {
                            friendshipIds.synchronous().invalidate(invitation.requester);
                            return DefaultDataObjects.addNetworkProfilesFriends(dataManager, gson, invitation.invited, id)
                                    .thenAccept(status3 -> friendshipIds.synchronous().invalidate(invitation.invited));
                        });
            }).thenAccept(v -> {
                invitations.invalidate(invitation);
                friendships.synchronous().put(id, invitation.convertRecord());
            });
        });
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
     * @param player the uuid of the player
     * @return a CompletableFuture that has a HashSet with the friendships, the HashSet is empty when the player has no friendships.
     */
    public CompletableFuture<HashSet<ControllerRelationship>> getFriendships(ControllerPlayer player) {
        // Fetch the friendship IDs
        return friendshipIds.get(player.getNetworkProfileUuid()).exceptionally(ex -> {
            if (ex instanceof NullPointerException) {
                return new ArrayList<>();
            }
            throw new CompletionException(ex);
        }).thenCompose(playerFriendshipIds -> {
            if (playerFriendshipIds == null || playerFriendshipIds.isEmpty()) {
                return CompletableFuture.completedFuture(new HashSet<>());
            }

            // Build requests per friendship
            ConcurrentHashMap.KeySetView<Long, Boolean> removal = ConcurrentHashMap.newKeySet();
            List<CompletableFuture<ControllerRelationship>> getFriendships = new ArrayList<>();
            for (Long friendId : playerFriendshipIds) {
                getFriendships.add(friendships.get(friendId).handle((friendship, friendshipEx) -> {
                    if (friendshipEx instanceof NullPointerException || friendship == null) {
                        removal.add(friendId);
                        return null;
                    }
                    return (friendshipEx == null) ? new ControllerRelationship(friendId, friendship.players()) : null;
                }));
            }

            // Run requests and combine them.
            return CompletableFuture.allOf(getFriendships.toArray(new CompletableFuture[0]))
                    .thenApply(v -> getFriendships.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toCollection(HashSet::new))
                    ).thenApply(friendships -> {
                        if (!removal.isEmpty()) {
                            List<Long> updatedIds = new ArrayList<>(playerFriendshipIds);
                            updatedIds.removeAll(removal);
                            DefaultDataObjects.setNetworkProfilesFriends(dataManager, player.getNetworkProfileUuid(), updatedIds);
                            friendshipIds.synchronous().put(player.getNetworkProfileUuid(), updatedIds);
                        }
                        return friendships;
                    });
        });
    }

    public CompletableFuture<Void> removeFriendship(ControllerRelationship friendship) {
        // Remove friendship data first
        DataObjectId friendshipDataObjectId = new DataObjectId(RelationalId.Friendships(friendship.getId()), "data");
        return DefaultDataObjects.setFriendshipsData(dataManager, gson, friendship.getId(), null).thenCompose(status -> {
            // Removed, remove from cache
            friendships.synchronous().invalidate(friendship.getId());
            // Now for both players remove it
            UUID player0 = friendship.players().stream().findFirst().orElse(null);
            UUID player1 = friendship.players().stream().filter(item -> !item.equals(player0)).findFirst().orElse(null);
            if (player0 != null) {
                // Remove from data
                return DefaultDataObjects.removeNetworkProfilesFriends(dataManager, gson, player0, friendship.getId())
                        .exceptionally(ex -> null).thenCompose(status2 -> {
                            friendshipIds.synchronous().invalidate(player0);
                            if (player1 != null) {
                                // Also remove for other player
                                return DefaultDataObjects.removeNetworkProfilesFriends(dataManager, gson, player1, friendship.getId())
                                        .thenAccept(status3 -> friendshipIds.synchronous().invalidate(player1));
                            }
                            // Other player is invalid, then we are done
                            return CompletableFuture.completedFuture(null);
                        });
            }
            // First player is invalid, that also means that the second player is also invalid. We are done
            return CompletableFuture.completedFuture(null);
        });
    }

    private CompletableFuture<Long> newId() {
        long newId = System.currentTimeMillis();
        // TODO Might overflow!
        return friendships.get(newId).exceptionally(ex -> {
            if (ex instanceof NullPointerException) {
                return null;
            }
            throw new CompletionException(ex);
        }).thenCompose(val -> {
            if (val == null) return CompletableFuture.completedFuture(newId);
            return newId();
        });
    }

    public record FriendshipInvitation(UUID requester, UUID invited) implements RecordConverter<RelationshipRecord> { // TODO Move?

        public FriendshipInvitation {
            Objects.requireNonNull(requester, "requester cannot be null");
            Objects.requireNonNull(invited, "invited cannot be null");
        }

        @Override
        public RelationshipRecord convertRecord() {
            return new RelationshipRecord(Set.of(requester, invited));
        }

    }

}
