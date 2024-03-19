package com.lahuca.lanecontrollervelocity.commands;

import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;
import com.lahuca.lanecontrollervelocity.VelocityController;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author _Neko1
 * @date 17.03.2024
 **/
public class PartyCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        if(!(invocation.source() instanceof Player player)) {
            invocation.source().sendPlainMessage("You must be a requested to run this command!"); //TODO: replace
            return;
        }

        String[] args = invocation.arguments();

        Optional<ControllerPlayer> optionalPlayer = Controller.getInstance().getPlayer(player.getUniqueId());
        Optional<ControllerParty> playerParty = optionalPlayer.flatMap(ControllerPlayer::getParty);

        if(args.length == 0) {

            /*
             *
             * /party <Player> - Sends a request to the requested
             * /party info - Sends an information about the party
             * /party accept <Player> - Accepts the request from the given requested
             * /party deny <Player> - Denies the request from the given requested
             * /party disband - Disbands the party
             * /party kick <Player> - Kicks the requested from the party
             * /party warp - Sends all players to the leader's server
             * /party leader <Player> - Passes the leader to the given requested [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
             *
             */

            //TODO: Send help message
            return;
        }

        if(args[0].equalsIgnoreCase("disband")) {
            playerParty.ifPresent(ControllerParty::disband);
            //TODO: Send message that party was disbanded
        }
        if(args[0].equalsIgnoreCase("warp")) {
            playerParty.ifPresent(party -> optionalPlayer.ifPresent(controllerPlayer -> Controller.getInstance().partyWarp(party, controllerPlayer.getGameId())));
            //TODO: Send message that party was warped
        } else if(args[0].equalsIgnoreCase("kick")) {
            if(args.length < 2) {
                //TODO: send help message
                // /party kick <Player>
                return;
            }

            String name = args[1];
            ControllerPlayer target = Controller.getInstance().getPlayerByName(name).orElse(null);

            playerParty.ifPresent(party -> {
                if(target != null && !party.players().contains(target)) {
                    //TODO: Send message that requested is not in requested's party
                    return;
                }

                party.removePlayer(target);
                //TODO: Send message that requested was kicked
            });
        } else if(args[0].equalsIgnoreCase("info")) {
            //Displays the information about the party: members, ?
            //TODO: Edit the message
            playerParty.ifPresent(party -> player.sendPlainMessage(party.toString()));
        } else if(args[0].equalsIgnoreCase("accept")) {
            if(args.length < 2) {
                //TODO: send help message
                // /party accept <Player>
                return;
            }

            String inviter = args[1];

            Controller.getInstance().getPlayerByName(inviter).flatMap(ControllerPlayer::getParty).ifPresent(controllerParty -> {
                if(!controllerParty.getInvited().contains(player.getUniqueId())) {
                    //TODO: send a message that he is not requested to join his party
                    return;
                }

                optionalPlayer.ifPresent(controllerParty::addPlayer);
                //TODO: send message that he accepted it
            });
        } else if(args[0].equalsIgnoreCase("deny")) {
            if(args.length < 2) {
                //TODO: send help message
                // /party deny <Player>
                return;
            }

            String inviter = args[1];

            Controller.getInstance().getPlayerByName(inviter).flatMap(ControllerPlayer::getParty).ifPresent(controllerParty -> {
                if(!controllerParty.getInvited().contains(player.getUniqueId())) {
                    //TODO: send a message that he is not requested to join his party
                    return;
                }

                optionalPlayer.ifPresent(controllerParty::removeRequest);
                //TODO: send message that he denied it
            });
        } else if(args[0].equalsIgnoreCase("leader")) {
            if(args.length < 2) {
                //TODO: send help message
                // /party leader <Player>
                return;
            }

            playerParty.ifPresent(party -> {
                if(!party.getOwner().equals(player.getUniqueId())) {
                    //TODO: send message that only owner can do this
                    return;
                }

                String newLeader = args[1];
                Optional<Player> leader = VelocityController.getInstance().getServer().getPlayer(newLeader);

                leader.ifPresent(leaderPlayer -> {
                    if(!party.contains(leaderPlayer.getUniqueId())) {
                        //TODO: send message that requested is not in the party
                        return;
                    }

                    party.setOwner(leaderPlayer.getUniqueId());
                });
            });
        } else {
            // /party <Player> - Will send invitation
            String name = args[0];

            Controller.getInstance().getPlayerByName(name).ifPresent(requested -> playerParty.ifPresent(controllerParty -> controllerParty.sendRequest(requested)));
            //TODO: Send message that requested was invited
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        List<String> possibilities = new ArrayList<>();
        if(!(invocation.source() instanceof Player player)) return possibilities;

        Optional<ControllerParty> controllerParty = Controller.getInstance().getPlayer(player.getUniqueId()).flatMap(ControllerPlayer::getParty);
        String[] args = invocation.arguments();

        if(args.length == 2 && (args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("leader"))) {
            List<String> partyMembers = new ArrayList<>();
            controllerParty.ifPresent(party -> party.players().forEach(partyPlayer -> partyMembers.add(partyPlayer.toString())));//TODO: fix the message its just UUID for now

            if(args[1].isEmpty()) {
                return partyMembers;
            }

            for(String current : partyMembers) {
                if(current.toLowerCase().startsWith(args[1].toLowerCase()) || args[1].toLowerCase().startsWith(current.toLowerCase())) {
                    possibilities.add(current);
                }
            }

            return possibilities;
        } else if(args.length == 1) {
            List<String> allNames = new ArrayList<>();
            VelocityController.getInstance().getServer().getAllPlayers().forEach(online -> controllerParty.ifPresent(party -> {
                if(!party.contains(online.getUniqueId())) allNames.add(online.getUsername());
            }));

            if(args[0].isEmpty()) return allNames;


            for(String current : allNames) {
                if(current.toLowerCase().startsWith(args[0].toLowerCase()) || args[0].toLowerCase().startsWith(current.toLowerCase())) {
                    possibilities.add(current);
                }
            }

            return possibilities;
        }

        return possibilities;
    }
}
