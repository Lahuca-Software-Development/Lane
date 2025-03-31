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
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.data.manager.FileDataManager;
import com.lahuca.lane.data.manager.MySQLDataManager;
import com.lahuca.lane.message.LaneMessage;
import com.lahuca.lane.message.MapLaneMessage;
import com.lahuca.lane.queue.*;
import com.lahuca.lanecontroller.*;
import com.lahuca.lanecontroller.events.QueueStageEvent;
import com.lahuca.lanecontroller.events.QueueStageEventResult;
import com.lahuca.lanecontrollervelocity.commands.FriendCommand;
import com.lahuca.lanecontrollervelocity.commands.PartyCommand;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Plugin(id = "lanecontrollervelocity", name = "Lane Controller Velocity", version = "1.0",
        url = "https://lahuca.com", description = "I did it!", authors = {"Lahuca Software Development (Laurenshup)", "_Neko1"})
public class VelocityController {

    private static VelocityController instance;

    public static final int port = 7766;
    public static final Gson gson = new GsonBuilder().create();
    public static final boolean useSSL = false;

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private VelocityControllerConfiguration configuration = null;
    private Controller controller = null;

    @Inject
    public VelocityController(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        instance = this;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        initializeConfig();
        // TODO Log whenever the config is the default one! So when it has made an error

        // Connection
        Connection connection = null;
        if(configuration.getConnection().getType() == VelocityControllerConfiguration.Connection.Type.SOCKET) {
            connection = new ServerSocketConnection(configuration.getConnection().getPort(), gson, configuration.getConnection().getSocket().isSsl());
        }

        // Data Manager
        DataManager dataManager = null;
        if(configuration.getDataManager().getType() == VelocityControllerConfiguration.DataManager.Type.FILE) {
            try {
                dataManager = new FileDataManager(gson, new File(dataDirectory.toFile(), configuration.getDataManager().getFile().getName()));
            } catch (FileNotFoundException e) {
                // We cannot start
                // TODO Log this!
                return;
            }
        } else if(configuration.getDataManager().getType() == VelocityControllerConfiguration.DataManager.Type.MYSQL) {
            VelocityControllerConfiguration.DataManager.MySQL mysqlConfig = configuration.getDataManager().getMysql();
            HikariConfig config = new HikariConfig();
            config.setMaxLifetime(1800000);
            config.setMinimumIdle(5);
            config.setIdleTimeout(600000);
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);
            config.addDataSourceProperty("autoReconnect", true);
            config.addDataSourceProperty("allowMultiQueries", true);
            config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl("jdbc:mysql://" + mysqlConfig.getHost() + ":" + mysqlConfig.getPort() + "/" + mysqlConfig.getDatabase());
            config.setUsername(mysqlConfig.getUsername());
            config.setPassword(mysqlConfig.getPassword());
            config.setLeakDetectionThreshold(60000L);
            config.setAutoCommit(true);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            dataManager = new MySQLDataManager(gson, new HikariDataSource(config), mysqlConfig.getPrefix());
        }

        if(connection == null || dataManager == null) {
            // TODO Log
            return;
        }
        try {
            controller = new Implementation(server, connection, dataManager);
        } catch (IOException e) {
            //TODO: Handle that exception
            e.printStackTrace();
        }

        CommandManager commandManager = server.getCommandManager();
        if(configuration.getCommands().isFriend()) {
            commandManager.register(commandManager.metaBuilder("friend").aliases("f", "friends").plugin(this).build(), new FriendCommand(this, controller, dataManager, gson).createBrigadierCommand());
        }
        if(configuration.getCommands().isParty()) {
            commandManager.register(commandManager.metaBuilder("party").aliases("p").plugin(this).build(), new PartyCommand());
        }
    }

    private void initializeConfig() {
        File file = new File(dataDirectory.toFile(), "config.toml");
        if(!file.exists()) {
            // File does not exist
            if(!file.getParentFile().exists()) {
                // Parent folder does not exist, create it.
                if(!file.getParentFile().mkdirs()) {
                    // We could not make parent folder, stop
                    configuration = new VelocityControllerConfiguration();
                    return;
                }
            }
            try {
                if(!file.createNewFile()) {
                    configuration = new VelocityControllerConfiguration();
                    return;
                }
            } catch (IOException e) {
                configuration = new VelocityControllerConfiguration();
                return;
            }
            // Create default
            boolean done = false;
            try (InputStream inputStream = getClass().getResourceAsStream("/config.toml")) {
                if (inputStream != null) {
                    Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    done = true;
                }
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
            if(!done) {
                configuration = new VelocityControllerConfiguration();
                return;
            }
        }
        // Load configuration
        try {
            Toml toml = new Toml().read(file);
            configuration = toml.to(VelocityControllerConfiguration.class);
        } catch (Exception ignored) {
            configuration = new VelocityControllerConfiguration();
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyShutdownEvent event) {
        if(controller != null) controller.shutdown();
    }

    public Optional<Controller> getController() {
        return Optional.ofNullable(controller);
    }

    /**
     * Retrieves the translated messages for this controller.
     * @param key the message key
     * @param locale the locale
     * @return the translated message
     */
    private String getMessage(String key, Locale locale) {
        return controller.getTranslator().retrieveMessage(key, locale);
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
        getController().ifPresentOrElse(ctrl -> {
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
        getController().ifPresentOrElse(ctrl -> ctrl.getPlayer(player.getUniqueId()).ifPresentOrElse(cPlayer -> {
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
        // TODO Move to controller probably?
        runOnControllerPlayer(event.getPlayer(), (controller, player) -> {
            System.out.println("DEBUG 0: " + player.toString());
            QueueRequest request = new QueueRequest(QueueRequestReason.NETWORK_JOIN, QueueRequestParameters.lobbyParameters);
            QueueStageEvent requestEvent = new QueueStageEvent(player, request);
            player.setQueueRequest(request);
            boolean nextStage = true;
            // We run the queue request as long as we are trying to find instances/games.
            while(nextStage) {
                nextStage = false;
                requestEvent.setNoneResult();
                controller.handleQueueStageEvent(requestEvent);
                QueueStageEventResult result = requestEvent.getResult();
                if(result instanceof QueueStageEventResult.QueueStageEventMessageableResult messageableResult) {
                    // We should disconnect the player.
                    TextComponent message;
                    if(messageableResult.getMessage() == null || messageableResult.getMessage().isEmpty()) {
                        message = Component.text(getMessage("cannotFindFreeInstance", player.getLanguage()));
                    } else {
                        message = Component.text(messageableResult.getMessage());
                    }
                    event.getPlayer().disconnect(message); // TODO Will this actually work here?
                    event.setInitialServer(null);
                    player.setQueueRequest(null);
                } else if(result instanceof QueueStageEventResult.QueueStageEventJoinableResult joinableResult) {
                    // We want to let the player join a specific instance or game.
                    ControllerLaneInstance instance;
                    String instanceId = null;
                    Long gameId = null;
                    // TODO playTogetherPlayers!
                    if(joinableResult instanceof QueueStageEventResult.JoinGame joinGame) {
                        gameId = joinGame.getGameId();
                        Optional<ControllerGame> gameOptional = controller.getGame(joinGame.getGameId());
                        if(gameOptional.isEmpty()) {
                            request.stages().add(new QueueStage(QueueStageResult.UNKNOWN_ID, null, joinGame.getGameId()));
                            nextStage = true;
                            continue;
                        }
                        ControllerGame game = gameOptional.get();
                        Optional<ControllerLaneInstance> instanceOptional = controller.getInstance(game.getInstanceId());
                        if(instanceOptional.isEmpty()) {
                            // Run the stage event again to determine a new ID.
                            request.stages().add(new QueueStage(QueueStageResult.INVALID_STATE, null, joinGame.getGameId()));
                            nextStage = true;
                            continue;
                        }
                        instance = instanceOptional.get();
                    } else {
                        QueueStageEventResult.JoinInstance joinInstance = (QueueStageEventResult.JoinInstance) result;
                        instanceId = joinInstance.getInstanceId();
                        Optional<ControllerLaneInstance> instanceOptional = controller.getInstance(joinInstance.getInstanceId());
                        if(instanceOptional.isEmpty()) {
                            // Run the stage event again to determine a new ID.
                            request.stages().add(new QueueStage(QueueStageResult.UNKNOWN_ID, joinInstance.getInstanceId(), null));
                            nextStage = true;
                            continue;
                        }
                        instance = instanceOptional.get();
                    }
                    if(!instance.isJoinable() || !instance.isNonPlayable() ||
                            instance.getCurrentPlayers() + 1 > instance.getMaxPlayers()) {
                        // Run the stage event again to find a joinable instance.
                        request.stages().add(new QueueStage(QueueStageResult.NOT_JOINABLE, instanceId, gameId));
                        nextStage = true;
                        continue;
                    }
                    // TODO Check whether the game actually has a place left. ONLY WHEN result is JoinGame
                    // We found a hopefully free instance, try do send the packet.
                    ControllerPlayerState state;
                    if(joinableResult instanceof QueueStageEventResult.JoinGame) {
                        state = new ControllerPlayerState(LanePlayerState.GAME_TRANSFER,
                                Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, instance.getId()),
                                        new ControllerStateProperty(LaneStateProperty.GAME_ID, gameId),
                                        new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                    } else {
                        state = new ControllerPlayerState(LanePlayerState.INSTANCE_TRANSFER,
                                Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, instance.getId()),
                                        new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                    }
                    player.setState(state); // TODO Better state handling!
                    player.setQueueRequest(request);
                    CompletableFuture<Result<Void>> future = controller.getConnection().<Void>sendRequestPacket((id) ->
                            new InstanceJoinPacket(id, player.convertRecord(), false, null), instance.getId()).getFutureResult();
                    try {
                        Result<Void> joinResult = future.get();
                        if(joinResult.isSuccessful()) {
                            Optional<RegisteredServer> instanceServer = server.getServer(instance.getId());
                            if(instanceServer.isEmpty()) {
                                request.stages().add(new QueueStage(QueueStageResult.SERVER_UNAVAILABLE, instanceId, gameId));
                                nextStage = true;
                                continue;
                            }
                            // We can join
                            event.setInitialServer(instanceServer.get());
                        } else {
                            // We are not allowing to join at this instance.
                            request.stages().add(new QueueStage(QueueStageResult.JOIN_DENIED, instanceId, gameId));
                            nextStage = true;
                            continue;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        // We did not receive a valid response.
                        request.stages().add(new QueueStage(QueueStageResult.NO_RESPONSE, instanceId, gameId));
                        nextStage = true;
                        continue;
                    }
                }
            }
        }, (message) -> {
            event.getPlayer().disconnect(message); // TODO Will this actually correct work here?
            event.setInitialServer(null);
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        // TODO Move to Controller
        runOnControllerPlayer(event.getPlayer(), (controller, player) -> {
            if(player.getState() == null) {
                // TODO Even after X seconds it should unregister: timer
                controller.unregisterPlayer(player.getUuid());
                return;
            }
            if(player.getState().getName().equals(LanePlayerState.OFFLINE)) {
                controller.unregisterPlayer(player.getUuid());
                return;
            }
            // TODO Even after X seconds it should unregister: timer
            controller.unregisterPlayer(player.getUuid());
        }, null);
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        // TODO, if the player is still on a lobby, we should revert the state to the lobby instead.
        // TODO, if the player was trying to join a game, we should retry it
        // TODO Maybe use event.getServerKickReason
        runOnControllerPlayer(event.getPlayer(), (controller, player) -> {
            QueueRequest request;
            if(player.getQueueRequest().isPresent()) {
                request = player.getQueueRequest().get();
                // The player is in a queue, but the last state has failed. Retrieve its data and fetch new stages.
                ControllerPlayerState playerState = player.getState();
                String instanceId = null;
                Long gameId = null;
                if(playerState != null && playerState.getProperties().containsKey(LaneStateProperty.INSTANCE_ID)) {
                    instanceId = String.valueOf(playerState.getProperties().get(LaneStateProperty.INSTANCE_ID).getValue());
                }
                if(playerState != null && playerState.getProperties().containsKey(LaneStateProperty.GAME_ID)) {
                    Object value = playerState.getProperties().get(LaneStateProperty.GAME_ID).getValue();
                    if(value instanceof Long parsed) gameId = parsed;
                }
                request.stages().add(new QueueStage(QueueStageResult.SERVER_KICKED, instanceId, gameId));
            } else {
                request = new QueueRequest(QueueRequestReason.SERVER_KICKED, QueueRequestParameters.lobbyParameters);
                player.setInstanceId(null);
                player.setGameId(null);
                player.setQueueRequest(request);
            }

            QueueStageEvent stageEvent = new QueueStageEvent(player, request);
            boolean nextStage = true;
            while(nextStage) {
                nextStage = false;
                stageEvent.setNoneResult();
                controller.handleQueueStageEvent(stageEvent);
                QueueStageEventResult result = stageEvent.getResult();
                if(result instanceof QueueStageEventResult.None none) {
                    TextComponent message;
                    if(none.getMessage() == null || none.getMessage().isEmpty()) {
                        message = Component.text(getMessage("none", player.getLanguage())); // TODO Different key!
                    } else {
                        message = Component.text(none.getMessage());
                    }
                    event.setResult(KickedFromServerEvent.Notify.create(message));
                    player.setQueueRequest(null);
                } else if(result instanceof QueueStageEventResult.Disconnect disconnect) {
                    TextComponent message;
                    if(disconnect.getMessage() == null || disconnect.getMessage().isEmpty()) {
                        message = Component.text(getMessage("disconnect", player.getLanguage())); // TODO Different key!
                    } else {
                        message = Component.text(disconnect.getMessage());
                    }
                    event.setResult(KickedFromServerEvent.DisconnectPlayer.create(message));
                    player.setQueueRequest(null);
                } else if(result instanceof QueueStageEventResult.QueueStageEventJoinableResult joinable) {
                    ControllerLaneInstance instance;
                    String resultInstanceId = null;
                    Long resultGameId = null;
                    // TODO playTogetherPlayers!
                    if(joinable instanceof QueueStageEventResult.JoinGame joinGame) {
                        resultGameId = joinGame.getGameId();
                        Optional<ControllerGame> gameOptional = controller.getGame(joinGame.getGameId());
                        if(gameOptional.isEmpty()) {
                            request.stages().add(new QueueStage(QueueStageResult.UNKNOWN_ID, null, joinGame.getGameId()));
                            nextStage = true;
                            continue;
                        }
                        ControllerGame game = gameOptional.get();
                        Optional<ControllerLaneInstance> instanceOptional = controller.getInstance(game.getInstanceId());
                        if(instanceOptional.isEmpty()) {
                            // Run the stage event again to determine a new ID.
                            request.stages().add(new QueueStage(QueueStageResult.UNKNOWN_ID, game.getInstanceId(), null));
                            nextStage = true;
                            continue;
                        }
                        instance = instanceOptional.get();
                    } else {
                        QueueStageEventResult.JoinInstance joinInstance = (QueueStageEventResult.JoinInstance) result;
                        resultInstanceId = joinInstance.getInstanceId();
                        Optional<ControllerLaneInstance> instanceOptional = controller.getInstance(joinInstance.getInstanceId());
                        if(instanceOptional.isEmpty()) {
                            // Run the stage event again to determine a new ID.
                            request.stages().add(new QueueStage(QueueStageResult.UNKNOWN_ID, joinInstance.getInstanceId(), null));
                            nextStage = true;
                            continue;
                        }
                        instance = instanceOptional.get();
                    }
                    if(instance.isJoinable() && instance.isNonPlayable() && instance.getCurrentPlayers() + 1 <= instance.getMaxPlayers()) {
                        // Run the stage event again to find a joinable instance.
                        request.stages().add(new QueueStage(QueueStageResult.NOT_JOINABLE, resultInstanceId, resultGameId));
                        nextStage = true;
                        continue;
                    }
                    // TODO Check whether the game actually has a place left. ONLY WHEN result is JoinGame
                    // We found a hopefully free instance, try do send the packet.
                    ControllerPlayerState state;
                    if(joinable instanceof QueueStageEventResult.JoinGame) {
                        state = new ControllerPlayerState(LanePlayerState.GAME_TRANSFER, Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, instance.getId()), new ControllerStateProperty(LaneStateProperty.GAME_ID, resultGameId), new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                    } else {
                        state = new ControllerPlayerState(LanePlayerState.INSTANCE_TRANSFER, Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, instance.getId()), new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                    }
                    player.setState(state); // TODO Better state handling!
                    player.setQueueRequest(request);
                    CompletableFuture<Result<Void>> future = controller.getConnection().<Void>sendRequestPacket((id) -> new InstanceJoinPacket(id, player.convertRecord(), false, null), instance.getId()).getFutureResult();
                    try {
                        Result<Void> joinResult = future.get();
                        if(joinResult.isSuccessful()) {
                            Optional<RegisteredServer> instanceServer = server.getServer(instance.getId());
                            if(instanceServer.isEmpty()) {
                                // TODO Should we let the Instance know that the player is not joining? Maybe they claimed a spot in the queue.
                                request.stages().add(new QueueStage(QueueStageResult.SERVER_UNAVAILABLE, resultInstanceId, resultGameId));
                                nextStage = true;
                                continue;
                            }
                            // We can join
                            event.setResult(KickedFromServerEvent.RedirectPlayer.create(instanceServer.get()));
                        } else {
                            // We are not allowing to join at this instance.
                            request.stages().add(new QueueStage(QueueStageResult.JOIN_DENIED, resultInstanceId, resultGameId));
                            nextStage = true;
                            continue;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        // We did not receive a valid response.
                        request.stages().add(new QueueStage(QueueStageResult.NO_RESPONSE, resultInstanceId, resultGameId));
                        nextStage = true;
                        continue;
                    }
                }
            }
            // The above is being run until either the player should be notified, disconnected or redirected.
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

    public static class Implementation extends Controller {

        private final ProxyServer server;

        public Implementation(ProxyServer server, Connection connection, DataManager dataManager) throws IOException {
            super(connection, dataManager);
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
                        Map.entry("cannotFindFreeInstance", "Cannot find free instance"),
                        Map.entry("disconnect", "Disconnecting")))));

        @Override
        public LaneMessage getTranslator() {
            return translator;
        }

        @Override
        public Optional<ControllerLaneInstance> getNewInstance(ControllerPlayer player, Collection<ControllerLaneInstance> exclude) {
            for(ControllerLaneInstance instance : getInstances()) {
                if(instance.isJoinable() && instance.isNonPlayable()
                        && instance.getCurrentPlayers() + 1 <= instance.getMaxPlayers() && !exclude.contains(instance)) {
                    return Optional.of(instance);
                }
            }
            return Optional.empty();
        }

        private boolean isInstanceJoinable(ControllerLaneInstance instance, int spots) {
            return instance.isJoinable() && instance.isNonPlayable() && instance.getCurrentPlayers() + spots <= instance.getMaxPlayers();
        }

        private Optional<ControllerGame> findByGameId(Object gameIdObject, HashSet<String> excludeInstances,
                                                      HashSet<Long> excludeGames, int spots) {
            if(gameIdObject instanceof Long gameId && !excludeGames.contains(gameId)) {
                Optional<ControllerGame> game = getGame(gameId);
                if(game.isPresent()) {
                    Optional<ControllerLaneInstance> instance = getInstance(game.get().getInstanceId());
                    return Optional.ofNullable(instance.isPresent() && !excludeInstances.contains(instance.get().getId()) && isInstanceJoinable(instance.get(), spots) ? game.get() : null);
                }
            }
            return Optional.empty();
        }

        private Optional<ControllerGame> findByGameData(Object instanceIdObject, Object gameTypeObject,
                                                        Object gameMapObject, Object gameModeObject, HashSet<String> excludeInstances,
                                                        HashSet<Long> excludeGames, int spots) {
            String instanceId = instanceIdObject instanceof String id ? id : null;
            if(instanceId != null && excludeInstances.contains(instanceId)) return Optional.empty();
            String gameType = gameTypeObject instanceof String type ? type : null;
            String gameMap = gameMapObject instanceof String map ? map : null;
            String gameMode = gameModeObject instanceof String mode ? mode : null;
            if(gameType == null && gameMap == null && gameMode == null) {
                return Optional.empty();
            }
            for(ControllerGame game : getGames()) {
                if(excludeInstances.contains(game.getInstanceId())) continue;
                if(excludeGames.contains(game.getGameId())) continue;
                if(instanceId != null && !game.getInstanceId().equals(instanceId)) continue;
                if(gameType != null && !game.getState().getName().equals(gameType)) continue;
                if(gameMap != null) {
                    if(game.getState().getProperties().containsKey(LaneStateProperty.GAME_MAP)) {
                        if(!game.getState().getProperties().get(LaneStateProperty.GAME_MAP).getValue().equals(gameMap)) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                if(gameMode != null) {
                    if(game.getState().getProperties().containsKey(LaneStateProperty.GAME_MODE)) {
                        if(!game.getState().getProperties().get(LaneStateProperty.GAME_MODE).getValue().equals(gameMap)) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                // We must have matched the given data. Check if spots are available
                Optional<ControllerLaneInstance> instance = getInstance(instanceId);
                if(instance.isPresent() && isInstanceJoinable(instance.get(), spots)) {
                    return Optional.of(game);
                }
            }
            return Optional.empty();
        }

        private Optional<ControllerLaneInstance> findByInstanceId(Object instanceIdObject, HashSet<String> excludeInstances, int spots) {
            if(instanceIdObject instanceof String instanceId && !excludeInstances.contains(instanceId)) {
                Optional<ControllerLaneInstance> instance = getInstance(instanceId);
                if(instance.isPresent()) {
                    return Optional.ofNullable(isInstanceJoinable(instance.get(), spots) ? instance.get() : null);
                }
            }
            return Optional.empty();
        }

        private Optional<ControllerLaneInstance> findByInstanceType(Object instanceTypeObject, HashSet<String> excludeInstances, int spots) {
            if(instanceTypeObject instanceof String instanceType) {
                for(ControllerLaneInstance instance : getInstances()) {
                    if(excludeInstances.contains(instance.getId())) continue;
                    if(instance.getType().isEmpty()) continue;
                    if(instance.getType().get().equals(instanceType) && isInstanceJoinable(instance, spots)) {
                        return Optional.of(instance);
                    }
                }
            }
            return Optional.empty();
        }

        /**
         * Fetch the correct games/instances and set the state.
         * @param event The event
         * @param useParty If we should also forward parties after this is done
         * @param allowExclude Exclude games/instances that have already been tried
         * @return True if an instance/game has been found
         */
        private boolean handleQueueStageEventParameters(QueueStageEvent event, boolean useParty, boolean allowExclude) {
            // Fetch potential party members
            HashSet<UUID> partyMembers = new HashSet<>();
            Optional<Long> partyIdOptional = event.getPlayer().getPartyId();
            if(useParty && partyIdOptional.isPresent()) {
                Optional<ControllerParty> partyOptional = getParty(partyIdOptional.get());
                if(partyOptional.isPresent()) {
                    ControllerParty party = partyOptional.get();
                    if(party.getOwner().equals(event.getPlayer().getUuid())) {
                        // The owner is trying to join, so we should join other players as well
                        for(UUID partyMemberUuid : party.getPlayers()) {
                            Optional<ControllerPlayer> partyMemberOptional = getPlayer(partyMemberUuid);
                            partyMemberOptional.ifPresent(player -> partyMembers.add(partyMemberUuid));
                        }
                    }
                }
            }

            // Fetch potential excluded instances/games
            HashSet<String> excludeInstances = new HashSet<>();
            HashSet<Long> excludeGames = new HashSet<>();
            if(allowExclude && !event.isInitialRequest()) {
                for(QueueStage stage : event.getQueueRequest().stages()) {
                    if(stage.instanceId() != null) excludeInstances.add(stage.instanceId());
                    if(stage.gameId() != null) excludeGames.add(stage.gameId());
                }
            }

            // Actually try to fetch a instance/game
            ArrayList<Set<QueueRequestParameter>> params = event.getQueueRequest().parameters().parameters();
            if(params.isEmpty()) {
                // We do not have the params to know what to do.
                event.setNoneResult();
                return false;
            }
            for(Set<QueueRequestParameter> priority : params) {
                if(priority.isEmpty()) continue;
                ArrayList<QueueRequestParameter> shuffled = new ArrayList<>(priority);
                Collections.shuffle(shuffled);
                for(QueueRequestParameter parameter : shuffled) {
                    HashMap<String, Object> data = parameter.data();
                    HashSet<UUID> joinTogether = partyMembers;
                    if(data.containsKey(QueueRequestParameter.partySkip) && data.get(QueueRequestParameter.partySkip) instanceof Boolean partySkip && partySkip) {
                        joinTogether = new HashSet<>();
                    }
                    // Find something with this parameter
                    if(data.containsKey(QueueRequestParameter.gameId)) {
                        Optional<ControllerGame> value = findByGameId(data.get(QueueRequestParameter.gameId), excludeInstances, excludeGames, joinTogether.size());
                        if(value.isPresent()) {
                            if(joinTogether.isEmpty()) event.setJoinGameResult(value.get().getGameId());
                            else event.setJoinGameResult(value.get().getGameId(), joinTogether);
                            return true;
                        }
                    }
                    if(data.containsKey(QueueRequestParameter.gameType) || data.containsKey(QueueRequestParameter.gameMap) || data.containsKey(QueueRequestParameter.gameMode)) {
                        Optional<ControllerGame> value = findByGameData(data.get(QueueRequestParameter.instanceId), data.get(QueueRequestParameter.gameType),
                                data.get(QueueRequestParameter.gameMap), data.get(QueueRequestParameter.gameMode), excludeInstances, excludeGames, joinTogether.size());
                        if(value.isPresent()) {
                            if(joinTogether.isEmpty()) event.setJoinGameResult(value.get().getGameId());
                            else event.setJoinGameResult(value.get().getGameId(), joinTogether);
                            return true;
                        }
                    }
                    if(data.containsKey(QueueRequestParameter.instanceId)) {
                        Optional<ControllerLaneInstance> value = findByInstanceId(data.get(QueueRequestParameter.instanceId), excludeInstances, joinTogether.size());
                        if(value.isPresent()) {
                            if(joinTogether.isEmpty()) event.setJoinInstanceResult(value.get().getId());
                            else event.setJoinInstanceResult(value.get().getId(), joinTogether);
                            return true;
                        }
                    }
                    if(data.containsKey(QueueRequestParameter.instanceType)) {
                        Optional<ControllerLaneInstance> value = findByInstanceType(data.get(QueueRequestParameter.instanceType), excludeInstances, joinTogether.size());
                        if(value.isPresent()) {
                            if(joinTogether.isEmpty()) event.setJoinInstanceResult(value.get().getId());
                            else event.setJoinInstanceResult(value.get().getId(), joinTogether);
                            return true;
                        }
                    }
                }
            }
            // We did not find anything that is left to join for the whole party and that has not tried before.
            event.setNoneResult();
            return false;
        }

        @Override
        public void handleQueueStageEvent(QueueStageEvent event) {
            // TODO Let other plugins handle this
            switch(event.getQueueRequest().reason()) {
                case NETWORK_JOIN, SERVER_KICKED -> {
                    if(!handleQueueStageEventParameters(event, false, true)) {
                        event.setDisconnectResult(); // TODO Maybe add message?
                    }
                }
                case PARTY_JOIN -> handleQueueStageEventParameters(event, false, true);
                case PLUGIN_INSTANCE, PLUGIN_CONTROLLER -> handleQueueStageEventParameters(event, true, true);
            }
        }

        @Override
        public boolean sendMessage(UUID player, String message) {
            Optional<Player> optionalPlayer = server.getPlayer(player);
            if(optionalPlayer.isEmpty()) return false;
            optionalPlayer.get().sendMessage(Component.text(message));
            return true;
        }

        @Override
        public boolean disconnectPlayer(UUID player, String message) {
            Optional<Player> optionalPlayer = server.getPlayer(player);
            if(optionalPlayer.isEmpty()) return false;
            optionalPlayer.get().disconnect(Component.text(message));
            return true;
        }

    }

}
