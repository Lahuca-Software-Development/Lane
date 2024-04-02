package com.lahuca.lanecontrollervelocity.commands;

import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontroller.ControllerPlayer;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import java.util.Optional;

/**
 * @author _Neko1
 * @date 16.03.2024
 **/
public class FriendCommand implements SimpleCommand {

    /**
     * Friend command:
     * /friend add <Player>
     * /friend remove <Player>
     * /friend accept <Player>
     * /friend deny <Player>
     * /friend list
     * <p>
     * Probably there should be some GUI that will open after running /friend
     **/

    @Override
    public void execute(Invocation invocation) {
        if(!(invocation.source() instanceof Player player)) {
            invocation.source().sendPlainMessage("You must be a requested to run this command!");
            return;
        }

        String[] args = invocation.arguments();

        Optional<ControllerPlayer> optionalPlayer = Controller.getInstance().getPlayer(player.getUniqueId());

        if(optionalPlayer.isEmpty()) {
            //TODO send message that smt is wrong.
            return;
        }

        ControllerPlayer controllerPlayer = optionalPlayer.get();

        if(args.length == 0) {
            //Send help message
            return;
        }

        if(args[0].equalsIgnoreCase("add")) {
            if(args.length < 2) {
                //Send add help message
                return;
            }

        } else if(args[0].equalsIgnoreCase("remove")) {
            if(args.length < 2) {
                //Send remove help message
                return;
            }


        } else if(args[0].equalsIgnoreCase("accept")) {
            if(args.length < 2) {
                //Send remove help message
                return;
            }

        } else if(args[0].equalsIgnoreCase("deny")) {
            if(args.length < 2) {
                //Send remove help message
                return;
            }

        } else if(args[0].equalsIgnoreCase("list")) {

        } else {
            //Send help message
        }
    }
}
