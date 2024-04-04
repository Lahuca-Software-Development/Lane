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
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.Result;
import com.lahuca.lane.connection.socket.server.ServerSocketConnection;
import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontroller.ControllerImplementation;
import com.lahuca.lanecontrollervelocity.commands.FriendCommand;
import com.lahuca.lanecontrollervelocity.commands.PartyCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ProxyServer;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
            new Controller(connection, new Implementation(server));
        } catch (IOException e) {
            //TODO: Handle that exception
            e.printStackTrace();
        }

        server.getCommandManager().register("friends", new FriendCommand(), "f", "friend");
        server.getCommandManager().register("party", new PartyCommand(), "p");
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

    public static class Implementation implements ControllerImplementation {

        private final ProxyServer server;

        public Implementation(ProxyServer server) {
            this.server = server;
        }

        @Override
        public CompletableFuture<Result<Void>> joinServer(UUID uuid, String destination) {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            // Fetch player
            server.getPlayer(uuid).ifPresentOrElse(player -> {
                // Fetch instance
                server.getServer(destination).ifPresentOrElse(instance -> {
                    // Do connection request
                    player.createConnectionRequest(instance).connect().whenComplete((result, ex) -> {
                        if(ex != null) {
                           future.completeExceptionally(ex);
                        }
                        // Transfer result
                        if(result.getStatus() == ConnectionRequestBuilder.Status.SUCCESS
                                || result.getStatus() == ConnectionRequestBuilder.Status.ALREADY_CONNECTED) {
                            future.complete(new Result<>(ResponsePacket.OK));
                        } else if(result.getStatus() == ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS) {
                            future.complete(new Result<>(ResponsePacket.CONNECTION_IN_PROGRESS));
                        } else if(result.getStatus() == ConnectionRequestBuilder.Status.CONNECTION_CANCELLED) {
                            future.complete(new Result<>(ResponsePacket.CONNECTION_CANCELLED));
                        } else if(result.getStatus() == ConnectionRequestBuilder.Status.SERVER_DISCONNECTED) {
                            future.complete(new Result<>(ResponsePacket.CONNECTION_DISCONNECTED));
                        } else {
                            future.complete(new Result<>(ResponsePacket.UNKNOWN));
                        }
                    });
                }, () -> future.complete(new Result<>(ResponsePacket.INVALID_ID)));
            }, () -> future.complete(new Result<>(ResponsePacket.INVALID_PLAYER)));
            return future;
        }

    }

}
