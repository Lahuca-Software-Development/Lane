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
import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.LaneStateProperty;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.packet.InstanceJoinPacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.Result;
import com.lahuca.lane.connection.socket.server.ServerSocketConnection;
import com.lahuca.lane.message.LaneMessage;
import com.lahuca.lane.message.MapLaneMessage;
import com.lahuca.lanecontroller.*;
import com.lahuca.lanecontrollervelocity.commands.FriendCommand;
import com.lahuca.lanecontrollervelocity.commands.PartyCommand;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Plugin(id = "lanecontrollervelocity", name = "Lane Controller Velocity", version = "1.0",
        url = "https://lahuca.com", description = "I did it!", authors = {"Lahuca Software Development (Laurenshup)", "_Neko1"})
public class VelocityController {

    private static VelocityController instance;

    public static final int port = 776;
    public static final Gson gson = new GsonBuilder().create();

    private final ProxyServer server;
    private final Logger logger;
    private final Implementation implementation;
    private final Connection connection;
    private Optional<Controller> controller = Optional.empty();

    @Inject
    public VelocityController(ProxyServer server, Logger logger) {
        instance = this;
        this.server = server;
        this.logger = logger;

        implementation = new Implementation(server);
        connection = new ServerSocketConnection(port, gson);

        try {
            controller = Optional.of(new Controller(connection, implementation));
        } catch (IOException e) {
            //TODO: Handle that exception
            e.printStackTrace();
        }

        server.getCommandManager().register("friends", new FriendCommand(), "f", "friend");
        server.getCommandManager().register("party", new PartyCommand(), "p");
    }

    /**
     * Retrieves the translated messages for this controller.
     * @param key the message key
     * @param locale the locale
     * @return the translated message
     */
    private String getMessage(String key, Locale locale) {
        return implementation.getTranslator().retrieveMessage(key, locale);
    }

    private Locale getLocale(Player player) {
        Locale locale = player.getEffectiveLocale();
        if(locale == null) locale = Locale.ENGLISH;
        return locale;
    }

    /**
     * When a proxy player is correctly authenticated, we first register the player to the controller.
     * @param event the login event
     */
    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        Locale locale = getLocale(player);
        controller.ifPresentOrElse(ctrl -> {
            String name = player.getUsername(); // TODO Load custom display name (maybe nicked name)?
            boolean registered = ctrl.registerPlayer(new ControllerPlayer(player.getUniqueId(), name, name, locale));
            if(!registered) {
                TextComponent message = Component.text(getMessage("failedToRegister", locale));
                event.setResult(ResultedEvent.ComponentResult.denied(message));// TODO Change key
            } else {
                event.setResult(ResultedEvent.ComponentResult.allowed());
            }
        }, () -> {
            TextComponent message = Component.text(getMessage("cannotReachController", locale));
            event.setResult(ResultedEvent.ComponentResult.denied(message));// TODO Change key
        });
    }

    private void runOnControllerPlayer(Player player, BiConsumer<Controller, ControllerPlayer> accept, Consumer<Component> failed) {
        Locale locale = getLocale(player);
        controller.ifPresentOrElse(ctrl -> ctrl.getPlayer(player.getUniqueId()).ifPresentOrElse(cPlayer -> {
            accept.accept(ctrl, cPlayer);
        }, () -> {
            if(failed != null) failed.accept(Component.text(getMessage("notRegisteredPlayer", locale)));
        }), () -> {
            if(failed != null) failed.accept(Component.text(getMessage("cannotReachController", locale)));
        });
    }

    /**
     * Since the player is registered, we are now sending the player to an available instance.
     * We loop through all available instances and per instance we check if the join is able to happen.
     * If it is able to happen, we send the player to there, otherwise we have to disconnect the player.
     * @param event the server select event
     */
    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        runOnControllerPlayer(event.getPlayer(), (controller, player) -> {
            HashSet<ControllerLaneInstance> exclude = new HashSet<>();
            AtomicBoolean done = new AtomicBoolean(false);
            AtomicBoolean success = new AtomicBoolean(false);
            do {
                implementation.getNewInstance(controller, player, exclude).ifPresentOrElse(instance -> {
                    ControllerPlayerState state = new ControllerPlayerState(LanePlayerState.INSTANCE_TRANSFER,
                            Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, instance.getId()),
                                    new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis()))); // TODO Better state handling!
                    player.setState(state);
                    CompletableFuture<Result<Void>> future = controller.buildUnsafeVoidPacket((id) ->
                            new InstanceJoinPacket(id, player.convertRecord(), false, null), instance.getId());
                    try {
                        Result<Void> result = future.get();
                        if(result.isSuccessful()) {
                            server.getServer(instance.getId()).ifPresentOrElse(server -> {
                                event.setInitialServer(server);
                                done.set(true);
                                success.set(true);
                            }, () -> exclude.add(instance));
                        } else {
                            exclude.add(instance);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        exclude.add(instance);
                    }
                }, () -> done.set(true));
            } while(!done.get());
            if(!success.get()) {
                TextComponent message = Component.text(getMessage("cannotFindFreeInstance", player.getLanguage()));
                event.getPlayer().disconnect(message); // TODO Will this actually correct work here?
                event.setInitialServer(null);
            }
        }, (message) -> {
            event.getPlayer().disconnect(message); // TODO Will this actually correct work here?
            event.setInitialServer(null);
        });
    }

    /**
     * For more dynamic programming we want to catch exactly when the player has connected to a server.
     * This updates the states from X_TRANSFER to X_TRANSFERRED.
     * @param event
     */
    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        runOnControllerPlayer(event.getPlayer(), (controller, player) -> {
            event.getPlayer().getCurrentServer().ifPresent(connection -> {
                String state = player.getState().getName();
                if(state.equals(LanePlayerState.INSTANCE_TRANSFER) || state.equals(LanePlayerState.GAME_TRANSFER)) {
                    // Correct state
                    String connectedWith = connection.getServer().getServerInfo().getName();
                    Object connectTo = player.getState().getProperties().get(LaneStateProperty.INSTANCE_ID).getValue();
                    if(connectTo instanceof String text && text.equals(connectedWith)) {
                        // Correctly transferred
                        HashMap<String, ControllerStateProperty> properties = player.getState().getProperties();
                        if(state.equals(LanePlayerState.INSTANCE_TRANSFER)) {
                            player.setState(new ControllerPlayerState(LanePlayerState.INSTANCE_TRANSFERRED,
                                    Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, properties.get(LaneStateProperty.INSTANCE_ID).getValue()),
                                            new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis()))));
                        } else {
                            player.setState(new ControllerPlayerState(LanePlayerState.GAME_TRANSFERRED,
                                    Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, properties.get(LaneStateProperty.INSTANCE_ID).getValue()),
                                            new ControllerStateProperty(LaneStateProperty.GAME_ID, properties.get(LaneStateProperty.GAME_ID).getValue()),
                                            new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis()))));
                        }
                    }
                } else {
                    // We are at the incorrect state
                    // TODO Make methods to handle this case, for now, we do nothing.
                }
            });
        }, null);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        runOnControllerPlayer(event.getPlayer(), (controller, player) -> {
            if(player.getState().getName().equals(LanePlayerState.OFFLINE)) {
                controller.unregisterPlayer(player.getUuid());
            }
            // TODO Even after X seconds it should unregister: timer
        }, null);
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        // TODO, if the player is still on a lobby, we should revert the state to the lobby instead.
        // TODO, if the player was trying to join a game, we should retry it
        runOnControllerPlayer(event.getPlayer(), (controller, player) -> {
            HashSet<ControllerLaneInstance> exclude = new HashSet<>();
            controller.getInstance(event.getServer().getServerInfo().getName()).ifPresent(exclude::add);
            AtomicBoolean done = new AtomicBoolean(false);
            AtomicBoolean success = new AtomicBoolean(false);
            do {
                implementation.getNewInstance(controller, player, exclude).ifPresentOrElse(instance -> {
                    ControllerPlayerState state = new ControllerPlayerState(LanePlayerState.INSTANCE_TRANSFER,
                            Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, instance.getId()),
                                    new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis()))); // TODO Better state handling!
                    player.setState(state);
                    CompletableFuture<Result<Void>> future = controller.buildUnsafeVoidPacket((id) ->
                            new InstanceJoinPacket(id, player.convertRecord(), false, null), instance.getId());
                    try {
                        Result<Void> result = future.get();
                        if(result.isSuccessful()) {
                            server.getServer(instance.getId()).ifPresentOrElse(server -> {
                                event.setResult(KickedFromServerEvent.RedirectPlayer.create(server)); // TODO Maybe message?
                                done.set(true);
                                success.set(true);
                            }, () -> exclude.add(instance));
                        } else {
                            exclude.add(instance);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        exclude.add(instance);
                    }
                }, () -> done.set(true));
            } while(!done.get());
            if(!success.get()) {
                TextComponent message = Component.text(getMessage("cannotFindFreeInstance", player.getLanguage()));
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(message));
                // TODO Maybe we should keep the player at the lobby?, but we somehow could not find one
            }
        }, (message) -> {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(message));
            // TODO Maybe we should keep the player at the lobby?, or register the player back
        });
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

        public static MapLaneMessage translator = new MapLaneMessage(Map.ofEntries(
                Map.entry(Locale.ENGLISH.toLanguageTag(), Map.ofEntries(
                        Map.entry("cannotReachController", "Cannot reach controller"),
                        Map.entry("failedToRegister", "Failed to register"),
                        Map.entry("notRegisteredPlayer", "Not registered player"),
                        Map.entry("cannotFindFreeInstance", "Cannot find free instance")))));

        @Override
        public LaneMessage getTranslator() {
            return translator;
        }

        @Override
        public Optional<ControllerLaneInstance> getNewInstance(Controller controller, ControllerPlayer player, Collection<ControllerLaneInstance> exclude) {
            for(ControllerLaneInstance instance : controller.getInstances()) {
                if(instance.isJoinable() && instance.isNonPlayable()
                        && instance.getCurrentPlayers() + 1 <= instance.getMaxPlayers() && !exclude.contains(instance)) {
                    return Optional.of(instance);
                }
            }
            return Optional.empty();
        }

    }

}
