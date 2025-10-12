package com.lahuca.lanecontrollervelocity.commands;

import com.lahuca.lane.queue.QueueRequestParameters;
import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontrollervelocity.VelocityController;
import com.mojang.brigadier.Command;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.proxy.Player;

/**
 * @author _Neko1
 * @date 16.03.2024
 **/
public class HubCommand {

    private final VelocityController velocityController;
    private final Controller controller;

    public HubCommand(VelocityController velocityController, Controller controller) {
        this.velocityController = velocityController;
        this.controller = controller;
    }

    public BrigadierCommand createBrigadierCommand() {
        return new BrigadierCommand(BrigadierCommand.literalArgumentBuilder("hub")
                .requires(commandSource -> commandSource instanceof Player)
                .executes(c -> {
                    if(!(c.getSource() instanceof Player player)) return -1;
                    controller.getPlayerManager().getPlayer(player.getUniqueId())
                            .ifPresent(controllerPlayer -> controllerPlayer.queue(QueueRequestParameters.lobbyParameters));
                    return Command.SINGLE_SUCCESS;
                }));
    }

}
