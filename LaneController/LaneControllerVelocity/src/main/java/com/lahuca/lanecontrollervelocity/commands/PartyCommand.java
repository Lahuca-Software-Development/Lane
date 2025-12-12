package com.lahuca.lanecontrollervelocity.commands;

import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;
import com.lahuca.lanecontrollervelocity.VelocityController;
import com.lahuca.lanecontrollervelocity.VelocityPlayerPair;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.translation.GlobalTranslator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 17.03.2024
 **/
public class PartyCommand { // TODO Probably want to set it to final, sealed, non-sealed everywhere!

    private final VelocityController velocityController;
    private final Controller controller;

    public PartyCommand(VelocityController velocityController, Controller controller) {
        this.velocityController = velocityController;
        this.controller = controller;
    }

    /*
     * /party <Player> - Sends a request to the requested [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
     * /party info - Sends an information about the party
     * /party accept <Player> - Accepts the request from the given requested
     * /party deny <Player> - Denies the request from the given requested
     * /party disband - Disbands the party [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
     * /party kick <Player> - Kicks the requested from the party [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
     * /party warp - Sends all players to the leader's server [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
     * /party leader <Player> - Passes the leader to the given requested [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
     * /party leave
     * /party join <player> - Join an open party
     * /party public - Disable invitations and enable it for everyone to join freely
     * /party private - Enable invitations and disable joining for everyone
     */

    public BrigadierCommand createBrigadierCommand() {
        // TODO Tab completion?
        Command<CommandSource> needArguments = c -> {
            // TODO Is this needed? Maybe auto?
            c.getSource().sendMessage(Component.translatable("lane.controller.commands.party.needArguments"));
            return Command.SINGLE_SUCCESS;
        };
        SuggestionProvider<CommandSource> partyMemberSuggestions = (c, builder) -> {
            if(!(c.getSource() instanceof Player player)) return builder.buildFuture();
            controller.getPlayerManager().getPlayer(player.getUniqueId()).flatMap(ControllerPlayer::getParty).ifPresent(party -> {
                party.getControllerPlayers().forEach(member -> builder.suggest(member.getUsername())); // TODO Display name?
            });
            return builder.buildFuture();
        };

        LiteralCommandNode<CommandSource> addCommand = BrigadierCommand.literalArgumentBuilder("add").executes(needArguments)
                .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word()).executes(addCommand())).build();
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("party")
                .requires(source -> source instanceof Player)
                .then(addCommand)
                .then(BrigadierCommand.literalArgumentBuilder("invite").redirect(addCommand))
                .then(BrigadierCommand.literalArgumentBuilder("accept").executes(needArguments)
                        .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word()).executes(acceptCommand())))
                .then(BrigadierCommand.literalArgumentBuilder("deny").executes(needArguments)
                        .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word()).executes(denyCommand())))
                .then(BrigadierCommand.literalArgumentBuilder("join").executes(needArguments)
                        .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word()).executes(joinCommand())))
                .then(BrigadierCommand.literalArgumentBuilder("disband").executes(disbandCommand()))
                .then(BrigadierCommand.literalArgumentBuilder("leader").executes(needArguments)
                        .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                                .suggests(partyMemberSuggestions)
                                .executes(leaderCommand())))
                .then(BrigadierCommand.literalArgumentBuilder("public").executes(publicPrivateCommand(true)))
                .then(BrigadierCommand.literalArgumentBuilder("private").executes(publicPrivateCommand(false)))
                .then(BrigadierCommand.literalArgumentBuilder("kick").executes(needArguments)
                        .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                                .suggests(partyMemberSuggestions)
                                .executes(kickCommand())))
                .then(BrigadierCommand.literalArgumentBuilder("leave").executes(leaveCommand()))
                .then(BrigadierCommand.literalArgumentBuilder("warp").executes(warpCommand()))
                .then(BrigadierCommand.literalArgumentBuilder("list").executes(listCommand()))
                .then(BrigadierCommand.literalArgumentBuilder("help").redirect(help()))
                .executes(help().getCommand())
                .build();
        return new BrigadierCommand(node);
    }

    // TODO Probably if a party member joins/leaves, then always send message to all party members!

    // Subcommands

    private LiteralCommandNode<CommandSource> help() {
        return BrigadierCommand.literalArgumentBuilder("help")
                .executes(c -> {
                    String input = c.getInput();
                    if(input.contains(" ")) {
                        input = input.split(" ")[0];
                    }
                    c.getSource().sendMessage(Component.translatable("lane.controller.commands.party.help", Component.text(input)));
                    return 1;
                })
                .build();
    }

    private Command<CommandSource> addCommand() {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, true);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS; // TODO Return invalid?
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();

            if(actors.target.getUuid().equals(executor.getUniqueId())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.add.yourself"));
                return Command.SINGLE_SUCCESS;
            }

            // Got players, get party
            ControllerParty party;
            Optional<ControllerParty> partyOpt = actors.executor.cPlayer().getParty();
            if (partyOpt.isPresent()) {
                party = partyOpt.get();
                // Already in a party
                if (!party.getOwner().equals(executor.getUniqueId())) { // Check if owner
                    executor.sendMessage(Component.translatable("lane.controller.commands.party.needOwner"));
                    return Command.SINGLE_SUCCESS;
                }
            } else {
                partyOpt = controller.getPartyManager().createParty(actors.executor.cPlayer());
                if (partyOpt.isPresent()) {
                    party = partyOpt.get();
                } else {
                    executor.sendMessage(Component.translatable("lane.controller.commands.party.add.unknown"));
                    return Command.SINGLE_SUCCESS;
                }
            }

            // We got rights to invite in the party object
            if (party.containsPlayer(actors.target.cPlayer())) { // Check if target already party member
                executor.sendMessage(Component.translatable("lane.controller.commands.party.add.alreadyMember"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.isInvitationsOnly()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.add.notInvitationOnly"));
                return Command.SINGLE_SUCCESS;
            }
            if (party.hasInvitation(actors.target.cPlayer())) { // Check if target already invited
                executor.sendMessage(Component.translatable("lane.controller.commands.party.add.alreadyInvited"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.addInvitation(actors.target.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.add.unknown"));
                return Command.SINGLE_SUCCESS;
            }
            // Woo hooo, invited!
            executor.sendMessage(Component.translatable("lane.controller.commands.party.add.invited", Component.text(actors.target.cPlayer().getUsername()))); // TODO Display name?
            Player target = actors.target().player();
            target.sendMessage(Component.translatable("lane.controller.commands.party.add.received", Component.text(executor.getUsername()))); // TODO Display name?
            target.sendMessage(Component.translatable("lane.controller.commands.party.add.accept", Component.text(executor.getUsername())).clickEvent(ClickEvent.runCommand("party accept " + executor.getUsername())));
            target.sendMessage(Component.translatable("lane.controller.commands.party.add.deny", Component.text(executor.getUsername())).clickEvent(ClickEvent.runCommand("party deny " + executor.getUsername())));
            return Command.SINGLE_SUCCESS;
        };
    }


    private Command<CommandSource> acceptCommand() {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, true);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS; // TODO Return invalid?
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();

            // Got players, get party
            if (actors.executor.cPlayer().getParty().isPresent()) {
                // Already in a party
                executor.sendMessage(Component.translatable("lane.controller.commands.party.accept.alreadyInParty"));
                return Command.SINGLE_SUCCESS;
            }
            Optional<ControllerParty> partyOpt = actors.target.cPlayer().getParty();
            if (partyOpt.isEmpty() || !partyOpt.get().getOwner().equals(actors.target.getUuid())) {
                // That target player does not have a party or is not the owner
                executor.sendMessage(Component.translatable("lane.controller.commands.party.accept.noInvitation"));
                return Command.SINGLE_SUCCESS;
            }

            // We can get party, check if we have rights to join it
            ControllerParty party = partyOpt.get();
            if (party.containsPlayer(actors.executor.cPlayer())) { // Check if target already party member
                executor.sendMessage(Component.translatable("lane.controller.commands.party.accept.alreadyMember"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.isInvitationsOnly()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.accept.notInvitationOnly"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.hasInvitation(actors.executor.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.accept.noInvitation"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.acceptInvitation(actors.executor.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.accept.unknown"));
            }
            // TODO Fix all of this: invitation check. This all!
            // Woo hooo, accepted!
            executor.sendMessage(Component.translatable("lane.controller.commands.party.accept.accepted", Component.text(actors.target.cPlayer().getUsername()))); // TODO Display name?
            Component joined = Component.translatable("lane.controller.commands.party.accept.joined", Component.text(actors.executor.cPlayer().getUsername())); // TODO Display name?
            party.getPlayers().forEach(partyMember -> {
                if (!partyMember.equals(executor.getUniqueId())) {
                    velocityController.getServer().getPlayer(partyMember).ifPresent(current -> current.sendMessage(joined));
                }
            });
            return Command.SINGLE_SUCCESS;
        };
    }

    private Command<CommandSource> denyCommand() {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, true);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS; // TODO Return invalid?
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();

            // Got players, get party
            Optional<ControllerParty> partyOpt = actors.target.cPlayer().getParty();
            if (partyOpt.isEmpty() || !partyOpt.get().getOwner().equals(actors.target.getUuid())) {
                // That target player does not have a party or is not the owner
                executor.sendMessage(Component.translatable("lane.controller.commands.party.deny.noInvitation"));
                return Command.SINGLE_SUCCESS;
            }

            // We can get party, check if we have been invited
            ControllerParty party = partyOpt.get();
            if (party.containsPlayer(actors.executor.cPlayer())) { // Check if target already party member
                executor.sendMessage(Component.translatable("lane.controller.commands.party.deny.alreadyMember"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.isInvitationsOnly()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.deny.notInvitationOnly"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.hasInvitation(actors.executor.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.deny.noInvitation"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.denyInvitation(actors.executor.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.deny.unknown"));
            }
            // TODO Fix all of this: invitation check. This all!
            // Woo hooo, denied!
            executor.sendMessage(Component.translatable("lane.controller.commands.party.deny.denied", Component.text(actors.target.cPlayer().getUsername()))); // TODO Display name?
            return Command.SINGLE_SUCCESS;
        };
    }

    private Command<CommandSource> joinCommand() {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, true);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS;
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();

            // Got players, get party
            if (actors.executor.cPlayer().getParty().isPresent()) {
                // Already in a party
                executor.sendMessage(Component.translatable("lane.controller.commands.party.join.alreadyInParty"));
                return Command.SINGLE_SUCCESS;
            }
            Optional<ControllerParty> partyOpt = actors.target.cPlayer().getParty();
            if (partyOpt.isEmpty() || !partyOpt.get().getOwner().equals(actors.target.getUuid())) {
                // That target player does not have a party or is not the owner
                // Do not leak if player has party, show a general message
                executor.sendMessage(Component.translatable("lane.controller.commands.party.join.unavailable"));
                return Command.SINGLE_SUCCESS;
            }

            // We can get party, check if we have been invited
            ControllerParty party = partyOpt.get();
            if (party.containsPlayer(actors.executor.cPlayer())) { // Check if target already party member
                executor.sendMessage(Component.translatable("lane.controller.commands.party.join.alreadyMember"));
                return Command.SINGLE_SUCCESS;
            }
            if (party.isInvitationsOnly()) {
                // Do not leak if player has party, show a general message
                executor.sendMessage(Component.translatable("lane.controller.commands.party.join.unavailable"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.joinPlayer(actors.executor.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.join.unknown"));
                return Command.SINGLE_SUCCESS;
            }
            executor.sendMessage(Component.translatable("lane.controller.commands.party.join.success", Component.text(actors.target.cPlayer().getUsername()))); // TODO Display name?
            Component joined = Component.translatable("lane.controller.commands.party.join.joined",
                    Component.text(actors.executor.cPlayer().getUsername()), Component.text(actors.target.cPlayer().getUsername())); // TODO Display name?
            // TODO CHECK THIS EVERYWHERE!!!
            party.getPlayers().forEach(partyMember -> {
                if (!partyMember.equals(executor.getUniqueId())) {
                    velocityController.getServer().getPlayer(partyMember).ifPresent(current -> current.sendMessage(joined));
                }
            });
            return Command.SINGLE_SUCCESS;
        };
    }

    private Command<CommandSource> disbandCommand() {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, false);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS;
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();

            // Got the executor, retrieve party
            Optional<ControllerParty> partyOpt = actors.executor.cPlayer().getParty();
            if (partyOpt.isEmpty()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.disband.needParty"));
                return Command.SINGLE_SUCCESS;
            }
            ControllerParty party = partyOpt.get();
            if (!party.getOwner().equals(actors.executor.getUuid())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.disband.notOwner"));
                return Command.SINGLE_SUCCESS;
            }
            HashSet<UUID> partyMembers = party.getPlayers();
            if (!party.disband()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.disband.unknown"));
                return Command.SINGLE_SUCCESS;
            }
            Component component = Component.translatable("lane.controller.commands.party.disband.disbanded");
            partyMembers.forEach(uuid -> velocityController.getServer().getPlayer(uuid).ifPresent(current -> current.sendMessage(component)));
            return Command.SINGLE_SUCCESS;
        };
    }

    private Command<CommandSource> leaderCommand() {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, true);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS; // TODO Return invalid?
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();

            // Got players, get party
            Optional<ControllerParty> partyOpt = actors.executor.cPlayer().getParty();
            if (partyOpt.isEmpty()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.leader.needParty"));
                return Command.SINGLE_SUCCESS;
            }
            ControllerParty party = partyOpt.get();
            if (!party.getOwner().equals(actors.executor.getUuid())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.leader.notOwner"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.containsPlayer(actors.target.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.leader.needPartyMember"));
                return Command.SINGLE_SUCCESS;
            }
            if (party.getOwner().equals(actors.target.getUuid())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.leader.yourself"));
                return Command.SINGLE_SUCCESS;
            }
            if (!party.setOwner(actors.target.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.leader.unknown"));
                return Command.SINGLE_SUCCESS;
            }
            Component component = Component.translatable("lane.controller.commands.party.leader.changed",
                    Component.text(actors.target.cPlayer().getUsername())); // TODO Displayname?
            party.getPlayers().forEach(uuid -> velocityController.getServer().getPlayer(uuid)
                    .ifPresent(current -> current.sendMessage(component)));
            return Command.SINGLE_SUCCESS;
        };
    }

    private Command<CommandSource> publicPrivateCommand(boolean publicCommand) {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, false);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS; // TODO Return invalid?
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();

            String subcommand = publicCommand ? "public" : "private";

            // Got players, get party
            Optional<ControllerParty> partyOpt = actors.executor.cPlayer().getParty();
            if (partyOpt.isEmpty()) {
                if(publicCommand) {
                    // Create new party
                    controller.getPartyManager().createParty(actors.executor.cPlayer()).ifPresentOrElse(party -> {
                        executor.sendMessage(Component.translatable("lane.controller.commands.party.createdParty",
                                Component.text(actors.executor.cPlayer().getUsername()))); // TODO Displayname?
                    }, () -> executor.sendMessage(Component.translatable("lane.controller.commands.party.public.unknown")));
                    return Command.SINGLE_SUCCESS;
                }
                executor.sendMessage(Component.translatable("lane.controller.commands.party." + subcommand + ".needParty"));
                return Command.SINGLE_SUCCESS;
            }
            ControllerParty party = partyOpt.get();
            if (!party.getOwner().equals(actors.executor.getUuid())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party." + subcommand + ".notOwner"));
                return Command.SINGLE_SUCCESS;
            }
            if((publicCommand && !party.isInvitationsOnly()) || (!publicCommand && party.isInvitationsOnly())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party." + subcommand + ".already"));
                return Command.SINGLE_SUCCESS;
            }
            party.setInvitationsOnly(!publicCommand);
            if(publicCommand) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.public.changed",
                        Component.text(actors.executor.cPlayer().getUsername()))); // TODO Displayname?
            } else {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.private.changed")); // TODO Displayname?
            }
            return Command.SINGLE_SUCCESS;
        };
    }

    private Command<CommandSource> kickCommand() {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, true);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS; // TODO Return invalid?
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();

            // Got players, get party
            Optional<ControllerParty> partyOpt = actors.executor.cPlayer().getParty();
            if (partyOpt.isEmpty()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.kick.needParty"));
                return Command.SINGLE_SUCCESS;
            }
            ControllerParty party = partyOpt.get();
            if (!party.getOwner().equals(actors.executor.getUuid())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.kick.notOwner"));
                return Command.SINGLE_SUCCESS;
            }
            if(party.getOwner().equals(actors.target.getUuid())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.kick.yourself"));
                return Command.SINGLE_SUCCESS;
            }
            if(!party.containsPlayer(actors.target.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.kick.needPartyMember"));
                return Command.SINGLE_SUCCESS;
            }
            if(!party.removePlayer(actors.target.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.kick.unknown"));
                return Command.SINGLE_SUCCESS;
            }
            executor.sendMessage(Component.translatable("lane.controller.commands.party.kick.success",
                    Component.text(actors.target.cPlayer().getUsername()))); // TODO Displayname?
            actors.target.player().sendMessage(Component.translatable("lane.controller.commands.party.kick.kicked"));
            return Command.SINGLE_SUCCESS;
        };
    }

    private Command<CommandSource> leaveCommand() {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, false);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS; // TODO Return invalid?
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();

            // Got players, get party
            Optional<ControllerParty> partyOpt = actors.executor.cPlayer().getParty();
            if (partyOpt.isEmpty()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.leave.needParty"));
                return Command.SINGLE_SUCCESS;
            }
            ControllerParty party = partyOpt.get();
            if(party.getOwner().equals(actors.executor.getUuid())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.leave.yourself"));
                return Command.SINGLE_SUCCESS;
            }
            if(!party.removePlayer(actors.executor.cPlayer())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.leave.unknown"));
                return Command.SINGLE_SUCCESS;
            }
            executor.sendMessage(Component.translatable("lane.controller.commands.party.leave.left"));
            Component component = Component.translatable("lane.controller.commands.party.leave.someone",
                    Component.text(actors.executor.cPlayer().getUsername())); // TODO Displayname?
            party.getPlayers().forEach(uuid -> velocityController.getServer().getPlayer(uuid).ifPresent(current -> current.sendMessage(component)));
            return Command.SINGLE_SUCCESS;
        };
    }

    private Command<CommandSource> warpCommand() {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, false);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS;
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();
            Optional<ControllerParty> partyOpt = actors.executor.cPlayer().getParty();
            if (partyOpt.isEmpty()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.warp.needParty"));
                return Command.SINGLE_SUCCESS;
            }
            ControllerParty party = partyOpt.get();
            if(!party.getOwner().equals(actors.executor.getUuid())) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.warp.notOwner"));
                return Command.SINGLE_SUCCESS;
            }
            // Okay, we got a party and are the owner, do the warp
            if(!party.warpParty()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.warp.unknown"));
                return Command.SINGLE_SUCCESS;
            }
            executor.sendMessage(Component.translatable("lane.controller.commands.party.warp.warped"));
            return Command.SINGLE_SUCCESS;
        };
    }

    private Command<CommandSource> listCommand() {
        return c -> {
            Optional<CommandActors> actorsOptional = fetchCommandActors(c, false);
            if (actorsOptional.isEmpty()) return Command.SINGLE_SUCCESS;
            CommandActors actors = actorsOptional.get();
            Player executor = actors.executor.player();
            Optional<ControllerParty> partyOpt = actors.executor.cPlayer().getParty();
            if (partyOpt.isEmpty()) {
                executor.sendMessage(Component.translatable("lane.controller.commands.party.list.needParty"));
                return Command.SINGLE_SUCCESS;
            }
            ControllerParty party = partyOpt.get();

            ArrayList<Component> nameComponents = new ArrayList<>();
            for (ControllerPlayer player : party.getControllerPlayers()) {
                nameComponents.add(GlobalTranslator.translator().translate(Component.translatable(
                        "lane.controller.commands.party.list.name",
                        Component.text(player.getUsername())
                ), executor.getEffectiveLocale()));
            }
            Component nameSeparator = GlobalTranslator.translator().translate(Component.translatable("lane.controller.commands.party.list.nameSeparator"),
                    executor.getEffectiveLocale());
            Component names = Component.join(JoinConfiguration.separator(nameSeparator), nameComponents);
            executor.sendMessage(Component.translatable("lane.controller.commands.party.list.style",
                    Component.text(party.getControllerOwner() == null ? "X" : party.getControllerOwner().getUsername()), names)); //TODO probably handle it better than "X" but this occurs. This should not even happen
            return Command.SINGLE_SUCCESS;
        };
    }

    // Helpers

    /**
     * A record containing the two player pairs that are mentioned when running the command.
     * This includes both the information about the executor and the person being targeted.
     * It is possible that there is only an executor involved, then the target values are null.
     *
     * @param executor the executor of the command
     * @param target   the target of the command
     */
    private record CommandActors(VelocityPlayerPair executor, VelocityPlayerPair target, String targetUsername) {
    }

    /**
     * Runs the logic to retrieve the command actors for the given command context.
     * This retrieves both the executor of the command and the person given in the command, if both are given.
     * When one of them is not found, but expected, the command source of the command context is notified.
     *
     * @param c         the command context
     * @param hasTarget {@code true} whether the command also has a target
     * @return an optionnal with the command actors, null when one of them fails
     */
    private Optional<CommandActors> fetchCommandActors(CommandContext<CommandSource> c, boolean hasTarget) {
        // Check if executed by a player
        if (!(c.getSource() instanceof Player player)) {
            c.getSource().sendMessage(Component.translatable("lane.controller.commands.error.playerRequired"));
            return Optional.empty();
        }

        // Fetch the player details
        Optional<VelocityPlayerPair> playerPair = velocityController.getPlayerPair(player.getUniqueId());
        if (playerPair.isEmpty()) {
            player.sendMessage(Component.translatable("lane.controller.commands.error.playerRequired"));
            return Optional.empty();
        }
        // Fetch the target details
        if (!hasTarget) {
            return Optional.of(new CommandActors(playerPair.get(), null, null));
        }
        String targetUsername = c.getArgument("player", String.class);

        Optional<VelocityPlayerPair> targetPair = velocityController.getPlayerPair(targetUsername, true);
        if (targetPair.isEmpty()) {
            player.sendMessage(Component.translatable("lane.controller.commands.error.unknownPlayer", Component.text(targetUsername)));
            return Optional.empty();
        }
        targetUsername = targetPair.get().cPlayer().getUsername();
        return Optional.of(new CommandActors(playerPair.get(), targetPair.get(), targetUsername));
    }

}
