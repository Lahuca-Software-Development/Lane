package com.lahuca.lanecontrollervelocity.commands;

import com.google.gson.Gson;
import com.lahuca.lane.FriendshipInvitation;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.records.RelationshipRecord;
import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontroller.ControllerFriendshipManager;
import com.lahuca.lanecontroller.ControllerPlayer;
import com.lahuca.lanecontrollervelocity.VelocityController;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    // TODO We might also want to seperate functionality: getFriendships, getInvitations, etc. To a different class

    private final VelocityController velocityController;
    private final Controller controller;
    private final DataManager dataManager;
    private final Gson gson;

    public FriendCommand(VelocityController velocityController, Controller controller, DataManager dataManager, Gson gson) {
        this.velocityController = velocityController;
        this.controller = controller;
        this.dataManager = dataManager;
        this.gson = gson;
    }

    // TODO Might want to set: "Retrieving information", when database is handled!

    private ControllerFriendshipManager manager() {
        return controller.getFriendshipManager();
    }

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
                    Optional<ControllerPlayer> optionalPlayer = controller.getPlayerManager().getPlayer(player.getUniqueId());
                    if(optionalPlayer.isEmpty()) {
                        player.sendPlainMessage("You need to be a controller player");
                        return Command.SINGLE_SUCCESS;
                    }
                    ControllerPlayer cPlayer = optionalPlayer.get();
                    manager().getFriendships(cPlayer).whenComplete((friendships, ex) -> {
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
                        for (RelationshipRecord friendship : friendships) {
                            Optional<UUID> other = friendship.players().stream().filter(uuid -> !uuid.equals(player.getUniqueId())).findFirst();
                            if (other.isEmpty())
                                continue; // TODO Whut. Do not send "Friends:" if we only have invalid data
                            // Check whether player with given UUID is online.
                            Optional<ControllerPlayer> friend = controller.getPlayerManager().getPlayer(other.get());
                            if (friend.isPresent()) {
                                // Player is online
                                ControllerPlayer friendPlayer = friend.get();
                                player.sendPlainMessage(friendPlayer.getUsername() + " is online");
                                // TODO Add if they are in a game, etc.
                            } else {
                                // Player is offline, fetch last username
                                controller.getPlayerManager().getPlayerUsername(other.get()).whenComplete((username, usernameEx) -> {
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
                .then(BrigadierCommand.literalArgumentBuilder("add") // TODO Add invite
                        .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                                .executes(c -> {
                                    if (!(c.getSource() instanceof Player player)) {
                                        c.getSource().sendPlainMessage("You must be in game to use this command");
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    Optional<ControllerPlayer> optionalPlayer = controller.getPlayerManager().getPlayer(player.getUniqueId());
                                    if(optionalPlayer.isEmpty()) {
                                        player.sendPlainMessage("You need to be a controller player");
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    ControllerPlayer cPlayer = optionalPlayer.get();
                                    // TODO: check if already friends, otherwise send invitation [IF player has enabled!]
                                    String username = c.getArgument("username", String.class);
                                    controller.getPlayerManager().getPlayerUuid(username).whenComplete((uuidOptional, ex) -> {
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
                                        for (Map.Entry<FriendshipInvitation, String> invitation : manager().getInvitations(cPlayer, true, true).entrySet()) {
                                            if (invitation.getKey().requester().equals(uuid)) {
                                                player.sendPlainMessage("This player has already given you an invite, accept with /friend accept");
                                                return;
                                            }
                                            if (invitation.getKey().invited().equals(uuid)) {
                                                player.sendPlainMessage("You have already given this player an invite.");
                                                return;
                                            }
                                        }
                                        // Not yet invited, check if friends already
                                        manager().getFriendships(cPlayer).whenComplete((friendships, friendshipsEx) -> {
                                            if (friendshipsEx != null) {
                                                player.sendMessage(Component.translatable("lane.controller.velocity.friend.error",
                                                        "Error happened while retrieving friends: " + friendshipsEx.getMessage()));
                                                return;
                                            }
                                            // Check whether we are already in a friendship
                                            for (RelationshipRecord friendship : friendships) {
                                                if (friendship.players().contains(uuid)) {
                                                    player.sendPlainMessage("You are already friends!");
                                                    return;
                                                }
                                            }
                                            // Not yet friends, check if
                                            velocityController.getPlayerPair(uuid).ifPresentOrElse(invited -> {
                                                manager().invite(new FriendshipInvitation(cPlayer.getNetworkProfileUuid(), invited.cPlayer().getNetworkProfileUuid()), player.getUsername()); // TODO Display?
                                                player.sendPlainMessage("Successfully invited " + username);
                                                invited.player().sendPlainMessage("Got friend invitation of " + player.getUsername() + ", it will become invalid in 1 minute"); // TODO Display
                                            }, () -> player.sendPlainMessage("The invited person is not online, cannot invite."));
                                        });
                                    });
                                    return Command.SINGLE_SUCCESS;
                                }))
                )
                .then(BrigadierCommand.literalArgumentBuilder("remove")
                        .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                                .executes(c -> {
                                    if (!(c.getSource() instanceof Player player)) {
                                        c.getSource().sendPlainMessage("You must be in game to use this command");
                                        return Command.SINGLE_SUCCESS; // TODO Return invalid
                                    }
                                    Optional<ControllerPlayer> optionalPlayer = controller.getPlayerManager().getPlayer(player.getUniqueId());
                                    if(optionalPlayer.isEmpty()) {
                                        player.sendPlainMessage("You need to be a controller player");
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    ControllerPlayer cPlayer = optionalPlayer.get();
                                    String username = c.getArgument("username", String.class);
                                    // Get UUID to username
                                    controller.getPlayerManager().getPlayerUuid(username).whenComplete((uuidOptional, ex) -> {
                                        if (ex != null) {
                                            player.sendPlainMessage("Error happened while retrieving friends: " + ex.getMessage());
                                            return;
                                        }
                                        if (uuidOptional.isEmpty()) {
                                            player.sendPlainMessage("Could not find player with given username");
                                            return;
                                        }
                                        // Get friendships
                                        manager().getFriendships(cPlayer).whenComplete((friendships, friendshipsEx) -> {
                                            if (friendshipsEx != null) {
                                                player.sendPlainMessage("Error happened while retrieving friends: " + ex.getMessage());
                                                return;
                                            }
                                            RelationshipRecord removal = null;
                                            for (RelationshipRecord friendship : friendships) {
                                                if (friendship.players().contains(uuidOptional.get())) {
                                                    // Found the friendship, remove
                                                    removal = friendship;
                                                    break;
                                                }
                                            }
                                            if (removal != null) {
                                                manager().removeFriendship(removal).whenComplete((status, removeEx) -> {
                                                    if (removeEx != null) {
                                                        player.sendPlainMessage("Error happened while removing friends: " + removeEx.getMessage());
                                                        return;
                                                    }
                                                    player.sendPlainMessage("Removed " + username + " from friendships");
                                                    // TODO Maybe also send message to other player, if they are online?
                                                });
                                                return;
                                            }
                                            player.sendPlainMessage("You are not friends with " + username);
                                        });
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
                                    Optional<ControllerPlayer> optionalPlayer = controller.getPlayerManager().getPlayer(player.getUniqueId());
                                    if(optionalPlayer.isEmpty()) {
                                        player.sendPlainMessage("You need to be a controller player");
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    ControllerPlayer cPlayer = optionalPlayer.get();
                                    String username = c.getArgument("username", String.class);
                                    // Get UUID to username
                                    controller.getPlayerManager().getPlayerUuid(username).whenComplete((uuidOptional, ex) -> {
                                        if (ex != null) {
                                            player.sendPlainMessage("Error happened while accepting friend: " + ex.getMessage());
                                            return;
                                        }
                                        if (uuidOptional.isEmpty()) {
                                            player.sendPlainMessage("Could not find player with given username");
                                            return;
                                        }
                                        // Assumed can be that a friendship is only invited whenever they are not friends yet.
                                        FriendshipInvitation invitation = new FriendshipInvitation(uuidOptional.get(), player.getUniqueId());
                                        String requesterUsername = manager().containsInvitation(invitation);
                                        if (requesterUsername == null) {
                                            player.sendPlainMessage("The player " + username + " has not invited you, or it has timed out");
                                            return;
                                        }
                                        // We can add to friends now.
                                        manager().acceptInvitation(invitation).whenComplete((status, statusEx) -> {
                                            if (statusEx != null) {
                                                player.sendPlainMessage("Error happened while accepting friend: " + statusEx.getMessage());
                                                return;
                                            }
                                            player.sendPlainMessage("You are now friends with " + requesterUsername);
                                            velocityController.getServer().getPlayer(uuidOptional.get()).ifPresent(requester -> {
                                                requester.sendPlainMessage("Friendship accepted: " + player.getUsername());
                                            });
                                        });
                                    });
                                    return Command.SINGLE_SUCCESS;
                                }))
                )
                .then(BrigadierCommand.literalArgumentBuilder("deny")
                        .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                                .executes(c -> {
                                    if (!(c.getSource() instanceof Player player)) {
                                        c.getSource().sendPlainMessage("You must be in game to use this command");
                                        return Command.SINGLE_SUCCESS; // TODO Return invalid
                                    }
                                    Optional<ControllerPlayer> optionalPlayer = controller.getPlayerManager().getPlayer(player.getUniqueId());
                                    if(optionalPlayer.isEmpty()) {
                                        player.sendPlainMessage("You need to be a controller player");
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    ControllerPlayer cPlayer = optionalPlayer.get();
                                    String username = c.getArgument("username", String.class);
                                    // Get UUID to username
                                    controller.getPlayerManager().getPlayerUuid(username).whenComplete((uuidOptional, ex) -> {
                                        if (ex != null) {
                                            player.sendPlainMessage("Error happened while denying friend: " + ex.getMessage());
                                            return;
                                        }
                                        if (uuidOptional.isEmpty()) {
                                            player.sendPlainMessage("Could not find player with given username");
                                            return;
                                        }
                                        // Assumed can be that a friendship is only invited whenever they are not friends yet.
                                        FriendshipInvitation invitation = new FriendshipInvitation(uuidOptional.get(), player.getUniqueId());
                                        String requesterUsername = manager().containsInvitation(invitation);
                                        if (requesterUsername == null) {
                                            player.sendPlainMessage("The player " + username + " has not invited you, or it has timed out");
                                            return;
                                        }
                                        // We can add to friends now.
                                        manager().invalidateInvitation(invitation);
                                        player.sendPlainMessage("Succesfully denied friendship invitation with " + requesterUsername);
                                        velocityController.getServer().getPlayer(uuidOptional.get()).ifPresent(requester -> {
                                            requester.sendPlainMessage("Friendship denied from: " + requesterUsername);
                                        });
                                    });
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(list)
                .then(help)
                .executes(help.getCommand())
                .build();

        return new BrigadierCommand(node);
    }

}
