package com.lahuca.lanecontrollervelocity.commands;

import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontroller.ControllerPlayer;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author _Neko1
 * @date 16.03.2024
 **/
public class FriendCommand implements SimpleCommand {

    /**
     * Friend command:
     * /friend add Player
     * /friend remove Player
     * /friend list
     * <p>
     * Probably there should be some GUI that will open after running /friend
     **/

    @Override
    public void execute(Invocation invocation) {
        if(!(invocation.source() instanceof Player player)) {
            invocation.source().sendPlainMessage("You must be a player to run this command!");
            return;
        }

        String[] args = invocation.arguments();

        Optional<ControllerPlayer> optionalPlayer = Controller.getInstance().getPlayer(player.getUniqueId());

        if(args.length == 0) {
            //Send help message
            return;
        }

        if(args[0].equalsIgnoreCase("add")) {
            if(args.length < 2) {
                //Send add help message
                return;
            }

            String name = args[1];

            optionalPlayer.ifPresent(controllerPlayer -> Controller.getInstance().getPlayerByName(name).ifPresent(controllerPlayer::addRelationship));
        } else if(args[0].equalsIgnoreCase("remove")) {
            if(args.length < 2) {
                //Send remove help message
                return;
            }

            String name = args[1];

            optionalPlayer.ifPresent(controllerPlayer -> Controller.getInstance().getPlayerByName(name).ifPresent(controllerPlayer::removeRelationship));
        } else if(args[0].equalsIgnoreCase("list")) {
            optionalPlayer.ifPresent(controllerPlayer -> {
                controllerPlayer.getRelationships().forEach(relationship -> {
                    player.sendMessage(Component.text("Friend: " + relationship.getTwo().getName()));
                });
            });
        } else {
            //Send help message
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source() instanceof Player;
    }
}
