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
            invocation.source().sendPlainMessage("You must be a player to run this command!"); //TODO: replace
            return;
        }

        String[] args = invocation.arguments();

        Optional<ControllerPlayer> optionalPlayer = Controller.getInstance().getPlayer(player.getUniqueId());
        Optional<ControllerParty> playerParty = optionalPlayer.flatMap(ControllerPlayer::getParty);

        if(args.length == 0) {
            //TODO: Send help message
            return;
        }

        if(args[0].equalsIgnoreCase("disband")) {
            playerParty.ifPresent(ControllerParty::disband);
            //TODO: Send message that party was disbanded
        } else if(args[0].equalsIgnoreCase("kick")) {
            if(args.length < 2) {
                //TODO: send help message
                // /party kick <Player>
                return;
            }

            String name = args[1];
            ControllerPlayer target = Controller.getInstance().getPlayerByName(name).orElse(null);

            playerParty.ifPresent(party -> {
                if(target != null && !party.getPlayers().contains(target)) {
                    //TODO: Send message that player is not in player's party
                    return;
                }

                party.removePlayer(target);
                //TODO: Send message that player was kicked
            });
        } else if(args[0].equalsIgnoreCase("info")) {
            //TODO: Edit the message
            playerParty.ifPresent(party -> player.sendPlainMessage(party.toString()));
        } else {
            // /party <Player> - Will send invitation
            String name = args[0];

            Controller.getInstance().getPlayerByName(name).ifPresent(requested -> playerParty.ifPresent(controllerParty -> controllerParty.sendRequest(requested)));
            //TODO: Send message that player was invited
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        List<String> possibilities = new ArrayList<>();
        if(!(invocation.source() instanceof Player player)) return possibilities;

        Optional<ControllerParty> controllerParty = Controller.getInstance().getPlayer(player.getUniqueId()).flatMap(ControllerPlayer::getParty);
        String[] args = invocation.arguments();

        if(args.length == 2 && args[0].equalsIgnoreCase("kick")) {
            List<String> partyMembers = new ArrayList<>();
            controllerParty.ifPresent(party -> party.getPlayers().forEach(partyPlayer -> partyMembers.add(partyPlayer.getName())));

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
