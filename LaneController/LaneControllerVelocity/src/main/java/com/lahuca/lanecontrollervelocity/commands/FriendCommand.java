package com.lahuca.lanecontrollervelocity.commands;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.data.RelationalId;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontroller.ControllerPlayer;
import com.lahuca.lanecontroller.ControllerRelationship;
import com.lahuca.lanecontrollervelocity.VelocityController;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author _Neko1
 * @date 16.03.2024
 **/
public class FriendCommand {

    /**
     * Friend command:
     * /friend add <Player> - Sends invite
     * /friend remove <Player> - Removes
     * /friend accept <Player> - Accepts invite
     * /friend deny <Player> - Denies invite
     * /friend allow - Allows retrieving invitations
     * /friend disallow - Disallows retrieving invitations
     * /friend list
     * /friend help
     * <p>
     * Probably there should be some GUI that will open after running /friend
     **/

    /*
    Friends are managed like this in the data manager, but then with UUIDs:

    players.Laurenshup.friends = [12345468887, 12654564789]
    players.KasprTv.friends = [12345468887]
    friends.12345468887.data = {"players" = ["Laurenshup", "KasprTv"]}
     */

    // TODO Add GUI, if wanted
    // TODO Maybe add some caching!

    private final VelocityController velocityController;
    private final Controller controller;
    private final DataManager dataManager;
    private final Gson gson;

    /**
     * A cache for saving the friendship IDs to a player.
     */
    private final AsyncLoadingCache<UUID, List<Long>> friendshipIds;
    /**
     * A cache for saving the friendship information identified by the ID of a friendship.
     */
    private final AsyncLoadingCache<Long, FriendshipDataObject> friendships; // TODO Move to Controller!
    /**
     * A cache for saving the usernames of players to their UUIDs.
     */
    private final AsyncLoadingCache<UUID, Optional<String>> uuidToOfflineUsername; // TODO We might want to store this in the controller?
    /**
     * A cache for saving the friend invites.
     * The key represents a combination of the two UUIDs, the value is the username of the requester.
     */
    private final Cache<FriendshipInvitePair, String> invitations;

    public FriendCommand(VelocityController velocityController, Controller controller, DataManager dataManager, Gson gson) {
        this.velocityController = velocityController;
        this.controller = controller;
        this.dataManager = dataManager;
        this.gson = gson;
        // TODO Probably change expiration times!
        friendshipIds = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).buildAsync((uuid, executor) -> {
            DataObjectId playerFriendListId = new DataObjectId(RelationalId.Players(uuid), "friends");
            // Read data object, if completable future is successful try to fetch the result.
            return dataManager.readDataObject(PermissionKey.CONTROLLER, playerFriendListId).thenApply(dataObjectOptional ->
                    dataObjectOptional.flatMap(object -> object.getValueAsLongArray(gson)).orElse(null));
        });
        friendships = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).buildAsync((friendshipId, executor) -> {
            DataObjectId friendshipDataObjectId = new DataObjectId(RelationalId.Friendships(friendshipId), "data");
            return dataManager.readDataObject(PermissionKey.CONTROLLER, friendshipDataObjectId).thenApply(dataObjectOptional ->
                    dataObjectOptional.flatMap(object -> object.getValueAsJson(gson, FriendshipDataObject.class)).orElse(null));
        });
        uuidToOfflineUsername = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).buildAsync((uuid, executor) ->
                controller.getOfflinePlayerName(uuid)); // TODO Probably want to move this to Controller!
        invitations = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
    }

    // TODO Might want to set: "Retrieving information", when database is handled!

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> help = BrigadierCommand.literalArgumentBuilder("help")
                .executes(c -> {
                    c.getSource().sendPlainMessage("Help");
                    return 1;
                })
                .build();
        LiteralCommandNode<CommandSource> list = BrigadierCommand.literalArgumentBuilder("list")
                .executes(c -> {
                    // List all friends
                    if (!(c.getSource() instanceof Player player)) {
                        c.getSource().sendPlainMessage("You must be in game to use this command");
                        return Command.SINGLE_SUCCESS; // TODO Return invalid
                    }
                    getFriendships(player.getUniqueId()).whenComplete((friendships, ex) -> {
                        if (ex != null) {
                            player.sendPlainMessage("Error happened while retrieving friends: " + ex.getMessage());
                            return;
                        }
                        if (friendships.isEmpty()) {
                            player.sendPlainMessage("You do not have any friends");
                            return;
                        }
                        player.sendPlainMessage("Friends:");
                        // TODO We might want to filter out the friends that are online first!
                        for (ControllerRelationship friendship : friendships) {
                            Optional<UUID> other = friendship.getPlayers().stream().filter(uuid -> !uuid.equals(player.getUniqueId())).findFirst();
                            if (other.isEmpty())
                                continue; // TODO Whut. Do not send "Friends:" if we only have invalid data
                            // Check whether player with given UUID is online.
                            Optional<ControllerPlayer> friend = controller.getPlayer(other.get());
                            if (friend.isPresent()) {
                                // Player is online
                                ControllerPlayer friendPlayer = friend.get();
                                player.sendPlainMessage(friendPlayer.getUsername() + " is online");
                                // TODO Add if they are in a game, etc.
                            } else {
                                // Player is offline, fetch last username
                                uuidToOfflineUsername.get(other.get()).whenComplete((username, usernameEx) -> {
                                    if (usernameEx != null || username.isEmpty()) return; // TODO Message?
                                    player.sendPlainMessage(username.get() + " is offline");
                                });
                            }
                        }

                    });
                    return Command.SINGLE_SUCCESS;
                })
                .build();
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("friend")
                .requires(source -> source instanceof Player)
                .then(BrigadierCommand.literalArgumentBuilder("add")
                        .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                                .executes(c -> {
                                    if (!(c.getSource() instanceof Player player)) {
                                        c.getSource().sendPlainMessage("You must be in game to use this command");
                                        return Command.SINGLE_SUCCESS; // TODO Return invalid
                                    }
                                    // TODO: check if already friends, otherwise send invitation [IF player has enabled!]
                                    String username = c.getArgument("username", String.class);
                                    controller.getPlayerUuid(username).whenComplete((uuidOptional, ex) -> {
                                        if (ex != null) {
                                            player.sendMessage(Component.translatable("lane.controller.velocity.friend.error",
                                                    "Error happened while retrieving friends: " + ex.getMessage()));
                                            return;
                                        }
                                        if (uuidOptional.isEmpty()) {
                                            player.sendMessage(Component.translatable("lane.controller.velocity.friend.unknownPlayer", "Could not find player with username"));
                                            return;
                                        }
                                        UUID uuid = uuidOptional.get();
                                        // Check if already invited
                                        for (Map.Entry<FriendshipInvitePair, String> invite : getInvitations(player.getUniqueId()).entrySet()) {
                                            if (invite.getKey().requester().equals(uuid)) {
                                                player.sendPlainMessage("This player has already given you an invite, accept with /friend accept");
                                                return;
                                            }
                                            if (invite.getKey().invited().equals(uuid)) {
                                                player.sendPlainMessage("You have already given this player an invite.");
                                                return;
                                            }
                                        }
                                        // Not yet invited, check if friends already
                                        getFriendships(player.getUniqueId()).whenComplete((friendships, friendshipsEx) -> {
                                            if (friendshipsEx != null) {
                                                player.sendMessage(Component.translatable("lane.controller.velocity.friend.error",
                                                        "Error happened while retrieving friends: " + friendshipsEx.getMessage()));
                                                return;
                                            }
                                            // Check whether we are already in a friendship
                                            for (ControllerRelationship friendship : friendships) {
                                                if (friendship.players().contains(uuid)) {
                                                    player.sendPlainMessage("You are already friends!");
                                                    return;
                                                }
                                            }
                                            // Not yet friends, check if
                                            velocityController.getServer().getPlayer(uuid).ifPresentOrElse(invited -> {
                                                invitations.put(new FriendshipInvitePair(player.getUniqueId(), uuid), player.getUsername()); // TODO Display?
                                                player.sendPlainMessage("Successfully invited " + username);
                                                invited.sendPlainMessage("Got friend invitation of " + player.getUsername() + ", it will become invalid in 1 minute"); // TODO Display
                                            }, () -> player.sendPlainMessage("The invited person is not online, cannot invite."));
                                        });
                                    });
                                    return Command.SINGLE_SUCCESS;
                                }))
                )
                .then(BrigadierCommand.literalArgumentBuilder("remove")
                        .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                                .executes(c -> {
                                    // List all friends
                                    if (!(c.getSource() instanceof Player player)) {
                                        c.getSource().sendPlainMessage("You must be in game to use this command");
                                        return Command.SINGLE_SUCCESS; // TODO Return invalid
                                    }
                                    String username = c.getArgument("username", String.class);
                                    // Get UUID to username
                                    controller.getPlayerUuid(username).whenComplete((uuidOptional, ex) -> {
                                        if (ex != null) {
                                            player.sendPlainMessage("Error happened while retrieving friends: " + ex.getMessage());
                                            return;
                                        }
                                        if (uuidOptional.isEmpty()) {
                                            player.sendPlainMessage("Could not find player with given username");
                                            return;
                                        }
                                        // Get friendships
                                        getFriendships(player.getUniqueId()).whenComplete((friendships, friendshipsEx) -> {
                                            if (friendshipsEx != null) {
                                                player.sendPlainMessage("Error happened while retrieving friends: " + ex.getMessage());
                                                return;
                                            }
                                            for (ControllerRelationship friendship : friendships) {
                                                if (friendship.players().contains(uuidOptional.get())) {
                                                    // Found the friendship, remove
                                                    // TODO
                                                }
                                            }
                                        });
                                    });

                                    // Go through the friendship IDs of the player to find the friendship that matches the one being removed.
                                    friendshipIds.get(player.getUniqueId()).whenComplete((playerFriendshipIds, ex) -> {

                                    });
                                    return Command.SINGLE_SUCCESS;
                                }))
                )
                .then(BrigadierCommand.literalArgumentBuilder("accept")
                        .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                                .executes(c -> {
                                    if (!(c.getSource() instanceof Player player)) {
                                        c.getSource().sendPlainMessage("You must be in game to use this command");
                                        return Command.SINGLE_SUCCESS; // TODO Return invalid
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))
                )
                .then(BrigadierCommand.literalArgumentBuilder("deny")
                        .executes(c -> {
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(list)
                .then(help)
                .executes(help.getCommand())
                .build();

        return new BrigadierCommand(node);
    }

    /**
     * Fetches the friendships of the player:
     * <ol>
     *     <li>First fetches the friendship IDs: players.PLAYER.friends.</li>
     *     <li>Then it combines the requests for the friendship data per ID: friends.ID.data.</li>
     *     <li>These requests are combined.<li></li>
     *     <li>Then removes any of the friendship IDs from players.PLAYER.friends that are not present anymore.</li>
     *     <li>Then returns the friendships that are present.</li>
     * </ol>
     * Any errors caught during the operation are parsed in the CompletableFuture returned.
     *
     * @param player the uuid of the player
     * @return a CompletableFuture that has a HashSet with the friendships, the HashSet is empty when the player has no friendships.
     */
    private CompletableFuture<HashSet<ControllerRelationship>> getFriendships(UUID player) {
        // Fetch the friendship IDs
        return friendshipIds.get(player).exceptionally(ex -> {
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
                    if (friendshipEx instanceof NullPointerException) {
                        removal.add(friendId);
                        return null;
                    }
                    return (friendshipEx == null) ? friendship.convertToRelationship(friendId) : null;
                }));
            }

            // Run requests and combine them.
            return CompletableFuture.allOf(getFriendships.toArray(new CompletableFuture[0]))
                    .thenApply(v -> getFriendships.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toCollection(HashSet::new))
                    )
                    .thenApply(friendships -> {
                        if (!removal.isEmpty()) {
                            List<Long> updatedIds = new ArrayList<>(playerFriendshipIds);
                            updatedIds.removeAll(removal);

                            DataObjectId playerFriendListId = new DataObjectId(RelationalId.Players(player), "friends");
                            dataManager.writeDataObject(PermissionKey.CONTROLLER,
                                    new DataObject(playerFriendListId, PermissionKey.CONTROLLER, gson, updatedIds));

                            friendshipIds.synchronous().put(player, updatedIds);
                        }
                        return friendships;
                    });
        });
    }

    /**
     * Fetches the friendship invitations where the given player is part of: either as requester or invited.
     * The fetched map has the invite pair together with the name of the requester.
     *
     * @param player the player to fetch the information of
     * @return the invitations
     */
    private HashMap<FriendshipInvitePair, String> getInvitations(UUID player) {
        HashMap<FriendshipInvitePair, String> invites = new HashMap<>();
        for (Map.Entry<FriendshipInvitePair, String> pair : invitations.asMap().entrySet()) {
            if (pair.getKey().invited().equals(player) || pair.getKey().requester().equals(player)) {
                invites.put(pair.getKey(), pair.getValue());
            }
        }
        return invites;
    }

    public record FriendshipDataObject(HashSet<UUID> players) {

        public ControllerRelationship convertToRelationship(long id) {
            return new ControllerRelationship(id, players);
        }

    }

    public record FriendshipInvitePair(UUID requester, UUID invited) {
    }

}
