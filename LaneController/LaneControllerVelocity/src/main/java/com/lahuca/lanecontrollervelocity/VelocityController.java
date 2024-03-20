/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 17:38 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontrollervelocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.socket.server.ServerSocketConnection;
import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontrollervelocity.commands.FriendCommand;
import com.lahuca.lanecontrollervelocity.commands.PartyCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

import java.io.IOException;
import java.util.logging.Logger;

@Plugin(id = "lanecontrollervelocity", name = "Lane Controller Velocity", version = "1.0",
        url = "https://lahuca.com", description = "I did it!", authors = {"Lahuca Software Development (Laurenshup)", "_Neko1"})
public class VelocityController {

    private static VelocityController instance;

    public static final int port = 776;
    public static final Gson gson = new GsonBuilder().create();

    private final ProxyServer server;
    private final Logger logger;

    private final Connection connection;

    @Inject
    public VelocityController(ProxyServer server, Logger logger) {
        instance = this;
        this.server = server;
        this.logger = logger;

        connection = new ServerSocketConnection(port, gson);

        try {
            new Controller(connection, (uuid, controllerGame) ->
                    server.getServer(controllerGame.getName()).ifPresent(registeredServer ->
                    server.getPlayer(uuid).ifPresent(player -> player.createConnectionRequest(registeredServer))));
        } catch(IOException e) {
            //TODO: Handle the exception
            e.printStackTrace();
        }

        server.getCommandManager().register("friends", new FriendCommand(), "f", "friend");
        server.getCommandManager().register("party", new PartyCommand(), "p");

        logger.info("Hello there! I made my first plugin with Velocity.");
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {

    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public static VelocityController getInstance() {
        return instance;
    }
}
