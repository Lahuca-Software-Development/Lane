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
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.UnsuccessfulResultException;
import com.lahuca.lane.connection.socket.server.ServerSocketConnection;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.data.manager.FileDataManager;
import com.lahuca.lane.data.manager.MySQLDataManager;
import com.lahuca.lane.queue.*;
import com.lahuca.lanecontroller.*;
import com.lahuca.lanecontroller.events.ControllerEvent;
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
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;

import java.io.*;
import java.nio.charset.StandardCharsets;
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

    // TODO Go through everything and clean up: final classes/functions (or sealed); public/private/etc.
    // TODO Either use ControllerPlayer everywhere, or use UUID. Not both. Maybe it is better to like make sure that ControllerPlayer objects cannot be consturcted
    // TODO Go through everything and make sure that some objects cannot be constructed.
    // TODO Go through everything and check whether the parsed parameters are correct: null checks, etc.
    // TODO Use requiresNonNull from Objects, instead! for parameters. Does NPE
    // TODO Fallback locale components
    // TODO Optional.filter!!

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

    public Optional<Controller> getController() {
        return Optional.ofNullable(controller);
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns a new pair of a Velocity player and Controller player by the given uuid.
     * @param uuid the uuid of the player
     * @return an optional with the pair
     * @throws IllegalArgumentException when {@code uuid} is null
     */
    public Optional<VelocityPlayerPair> getPlayerPair(UUID uuid) {
        if (uuid == null) throw new IllegalArgumentException("uuid cannot be null");
        return getServer().getPlayer(uuid).flatMap(player ->
                getController().flatMap(control ->
                        control.getPlayerManager().getPlayer(uuid)).map(cPlayer ->
                        new VelocityPlayerPair(player, cPlayer)));
    }

    /**
     * Returns a new pair of a Velocity player and Controller player by the given username.
     * The retrieved Velocity player is always checked case insensitively.
     * @param username the username of the player
     * @param caseInsensitive whether the Controller player should be searched case insensitively
     * @return an optional with the pair
     * @throws IllegalArgumentException when {@code username} is null
     */
    public Optional<VelocityPlayerPair> getPlayerPair(String username, boolean caseInsensitive) {
        if (username == null) throw new IllegalArgumentException("username cannot be null");
        return getServer().getPlayer(username).flatMap(player ->
                getController().flatMap(control ->
                        control.getPlayerManager().getPlayerByUsername(username, caseInsensitive)).map(cPlayer ->
                        new VelocityPlayerPair(player, cPlayer)));
    }

    public static VelocityController getInstance() {
        return instance;
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        initializeConfig();
        initializeResourceBundles();
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
            commandManager.register(commandManager.metaBuilder("party").aliases("p").plugin(this).build(), new PartyCommand(this, controller).createBrigadierCommand());
        }
    }

    private void initializeConfig() {
        // Config
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

    private void initializeResourceBundles() {
        // Resource Bundles
        File resourceBundleFolder = new File(dataDirectory.toFile(), "lang");
        File defaultResourceBundle = new File(resourceBundleFolder, "messages_en.properties");
        // TODO Here, where it did not work! do something like exception
        if(!defaultResourceBundle.exists()) {
            // File does not exist
            if(!defaultResourceBundle.getParentFile().exists()) {
                // Parent folder does not exist, create it.
                if(!defaultResourceBundle.getParentFile().mkdirs()) {
                    // We could not make parent folder, stop
                    return;
                }
            }
            try {
                if(!defaultResourceBundle.createNewFile()) {
                    return;
                }
            } catch (IOException e) {
                return;
            }
            // Create default
            boolean done = false;
            try (InputStream inputStream = getClass().getResourceAsStream("/messages.properties")) {
                if (inputStream != null) {
                    Files.copy(inputStream, defaultResourceBundle.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    done = true;
                }
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
            if(!done) {
                return;
            }
        }
        loadResourceBundles(resourceBundleFolder, MiniMessageTranslationStore.create(Key.key("lane:controller")));
    }

    private static void loadResourceBundles(File langFolder, MiniMessageTranslationStore store) {
        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".properties"));
        if (files == null) return;

        for (File file : files) {
            Locale locale = parseLocale(file.getName());

            try (InputStream in = new FileInputStream(file);
                 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                ResourceBundle bundle = new PropertyResourceBundle(reader);
                store.registerAll(locale, bundle, true);
            } catch (IOException e) {
                e.printStackTrace(); // TODO Hmmm
            }
        }
        GlobalTranslator.translator().addSource(store);
    }

    private static Locale parseLocale(String filename) {
        // messages_en_US_v.properties → en_US_v
        String base = filename.replace(".properties", "");

        // Remove prefix like "messages" (everything before first underscore)
        int underscoreIndex = base.indexOf('_');
        if (underscoreIndex == -1) return Locale.ROOT;

        String localePart = base.substring(underscoreIndex + 1); // get just "en", "en_US", etc.
        String[] parts = localePart.split("_");

        return switch (parts.length) {
            case 1 -> Locale.of(parts[0]); // en
            case 2 -> Locale.of(parts[0], parts[1]); // en_US
            case 3 -> Locale.of(parts[0], parts[1], parts[2]); // en_US_v
            default -> Locale.ROOT;
        };
    }


    @Subscribe
    public void onProxyInitialization(ProxyShutdownEvent event) {
        if(controller != null) controller.shutdown();
    }

    /**
     * When a proxy player is correctly authenticated, we first register the player to the controller.
     * @param event the login event
     */
    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        getController().ifPresentOrElse(controller -> {
            String name = player.getUsername(); // TODO Load custom display name (maybe nicked name)?
            try {
                Locale effectiveLocale = controller.getPlayerManager().registerPlayer(player.getUniqueId(), name, Locale.forLanguageTag(configuration.getDefaultLocale()));
                if(effectiveLocale == null) {
                    TranslatableComponent message = Component.translatable("lane.controller.error.login.register"); // TODO This will always use English?
                    event.setResult(ResultedEvent.ComponentResult.denied(message));
                } else {
                    player.setEffectiveLocale(effectiveLocale);
                    event.setResult(ResultedEvent.ComponentResult.allowed());
                }
            } catch (InterruptedException | ExecutionException e) {
                TranslatableComponent message = Component.translatable("lane.controller.error.login.invalidSetup");
                event.setResult(ResultedEvent.ComponentResult.denied(message));
            }
        }, () -> {
            TranslatableComponent message = Component.translatable("lane.controller.error.controller.unavailable");
            event.setResult(ResultedEvent.ComponentResult.denied(message));
        });
    }

    private void runOnControllerPlayer(Player player, BiConsumer<Controller, ControllerPlayer> accept, Consumer<Component> failed) {
        getController().ifPresentOrElse(ctrl -> ctrl.getPlayer(player.getUniqueId()).ifPresentOrElse(cPlayer -> {
            accept.accept(ctrl, cPlayer);
        }, () -> {
            if(failed != null) failed.accept(Component.translatable("lane.controller.error.player.unregistered"));
        }), () -> {
            if(failed != null) failed.accept(Component.translatable("lane.controller.error.controller.unavailable"));
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
        // TODO Definitely go over this and the queue from the ServerKickedEvent, it is not good to repeat everything!
        // TODO Move to controller probably?
        runOnControllerPlayer(event.getPlayer(), (controller, player) -> {
            QueueRequest request = new QueueRequest(QueueRequestReason.NETWORK_JOIN, QueueRequestParameters.lobbyParameters);
            QueueStageEvent stageEvent = new QueueStageEvent(player, request);
            // Due to event handling, we need to do this synced!
            // We use a while loop to handle failures within this method.
            boolean nextStage;
            do {
                nextStage = false;
                try {
                    player.handleQueueStage(stageEvent, false).get();
                    switch (stageEvent.getResult()) {
                        case QueueStageEventResult.QueueStageEventMessageableResult messageable -> {
                            // We should disconnect the player.
                            Component message = messageable.getMessage().orElse(Component.translatable("lane.controller.error.login.unavailable"));
                            event.getPlayer().disconnect(message); // TODO Will this actually work here?
                            event.setInitialServer(null);
                        }
                        case QueueStageEventResult.QueueStageEventJoinableResult joinable -> {
                            // Fetch instance ID and potential game ID
                            String instanceId;
                            Long gameId = null;
                            if(joinable instanceof QueueStageEventResult.JoinInstance joinInstance) {
                                instanceId = joinInstance.instanceId();
                            } else if(joinable instanceof QueueStageEventResult.JoinGame joinGame) {
                                gameId = joinGame.gameId();
                                instanceId = controller.getGame(joinGame.gameId()).map(ControllerGame::getInstanceId).orElse(null);
                            } else {
                                request.stages().add(new QueueStage(QueueStageResult.INVALID_STATE, joinable.getQueueType(), null, null));
                                nextStage = true;
                                continue;
                            }

                            Optional<RegisteredServer> instanceServer = server.getServer(instanceId);
                            if (instanceServer.isEmpty()) {
                                // TODO Should we let the Instance know that the player is not joining? Maybe they claimed a spot in the queue.
                                request.stages().add(new QueueStage(QueueStageResult.SERVER_UNAVAILABLE, joinable.getQueueType(), instanceId, gameId));
                                nextStage = true;
                                continue;
                            }
                            // We can join, set the initial server and call network processor
                            event.setInitialServer(instanceServer.get());
                            controller.getPlayerManager().doNetworkProcessing(player);

                            // Let party members also join
                            // TODO This below, should we do that really? OR AFTER PROCESSING?!?!?!?
                            if (joinable.getJoinTogetherPlayers() != null && !joinable.getJoinTogetherPlayers().isEmpty()) {
                                QueueRequestParameter partyJoinParameter;
                                if (gameId != null) {
                                    partyJoinParameter = QueueRequestParameter.create().gameId(gameId).instanceId(instanceId).build();
                                } else {
                                    partyJoinParameter = QueueRequestParameter.create().instanceId(instanceId).build();
                                }
                                QueueRequest partyRequest = new QueueRequest(QueueRequestReason.PARTY_JOIN, QueueRequestParameters.create().add(partyJoinParameter).build());
                                joinable.getJoinTogetherPlayers().forEach(uuid -> Controller.getPlayer(uuid).ifPresent(controllerPlayer -> controllerPlayer.queue(partyRequest)));
                            }
                        }
                        default -> {
                            // Very unfortunate, got strange state.
                            Component message = Component.translatable("lane.controller.error.login.unavailable");
                            event.getPlayer().disconnect(message);
                            event.setInitialServer(null);
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // Very unfortunate, we cannot retrieve it.
                    Component message = Component.translatable("lane.controller.error.login.unavailable");
                    event.getPlayer().disconnect(message);
                    event.setInitialServer(null);
                }
            } while(nextStage);
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
                controller.getPlayerManager().unregisterPlayer(player.getUuid());
                return;
            }
            if(player.getState().getName().equals(LanePlayerState.OFFLINE)) {
                controller.getPlayerManager().unregisterPlayer(player.getUuid());
                return;
            }
            // TODO Even after X seconds it should unregister: timer
            controller.getPlayerManager().unregisterPlayer(player.getUuid());
        }, null);
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        // TODO, if the player is still on a lobby, we should revert the state to the lobby instead.
        // TODO, if the player was trying to join a game, we should retry it
        // TODO Maybe use event.getServerKickReason
        runOnControllerPlayer(event.getPlayer(), (controller, player) -> {
            // Create a new request, or update if it already exists.
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
                QueueType queueType = QueueType.PLAYING; // TODO this is constant, but it should use the last one. Prpbs dont want to write in state?!
                request.stages().add(new QueueStage(QueueStageResult.SERVER_KICKED, event.getServerKickReason().orElse(null), queueType, instanceId, gameId));
            } else {
                request = new QueueRequest(QueueRequestReason.SERVER_KICKED, event.getServerKickReason().orElse(null), QueueRequestParameters.lobbyParameters);
                player.setInstanceId(null);
                player.setGameId(null);
            }

            // Due to event handling, we need to do this synced!
            // We use a while loop to handle failures within this method.
            boolean nextStage;
            QueueStageEvent stageEvent = new QueueStageEvent(player, request);
            do {
                nextStage = false;
                try {
                    player.handleQueueStage(stageEvent, false).get();
                    switch (stageEvent.getResult()) {
                        case QueueStageEventResult.None none -> {
                            Component message = none.getMessage().orElse(Component.translatable("lane.controller.error.queue.kicked.none")); // TODO Add more information: kick reason, what server, during join?
                            event.setResult(KickedFromServerEvent.Notify.create(message));
                        }
                        case QueueStageEventResult.Disconnect disconnect -> {
                            Component message = disconnect.getMessage().orElse(Component.translatable("lane.controller.error.queue.kicked.disconnect")); // TODO Add more information: kick reason, what server, during join?
                            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(message));
                        }
                        case QueueStageEventResult.QueueStageEventJoinableResult joinable -> {
                            // Fetch instance ID and potential game ID
                            String instanceId;
                            Long gameId = null;
                            if(joinable instanceof QueueStageEventResult.JoinInstance joinInstance) {
                                instanceId = joinInstance.instanceId();
                            } else if(joinable instanceof QueueStageEventResult.JoinGame joinGame) {
                                gameId = joinGame.gameId();
                                instanceId = controller.getGame(joinGame.gameId()).map(ControllerGame::getInstanceId).orElse(null);
                            } else {
                                request.stages().add(new QueueStage(QueueStageResult.INVALID_STATE, joinable.getQueueType(), null, null));
                                nextStage = true;
                                continue;
                            }

                            Optional<RegisteredServer> instanceServer = server.getServer(instanceId);
                            if (instanceServer.isEmpty()) {
                                // TODO Should we let the Instance know that the player is not joining? Maybe they claimed a spot in the queue.
                                request.stages().add(new QueueStage(QueueStageResult.SERVER_UNAVAILABLE, joinable.getQueueType(), instanceId, gameId));
                                nextStage = true;
                                continue;
                            }
                            // We can join
                            event.setResult(KickedFromServerEvent.RedirectPlayer.create(instanceServer.get()));

                            // Let party members also join
                            if (joinable.getJoinTogetherPlayers() != null && !joinable.getJoinTogetherPlayers().isEmpty()) {
                                QueueRequestParameter partyJoinParameter;
                                if (gameId != null) {
                                    partyJoinParameter = QueueRequestParameter.create().gameId(gameId).instanceId(instanceId).build();
                                } else {
                                    partyJoinParameter = QueueRequestParameter.create().instanceId(instanceId).build();
                                }
                                QueueRequest partyRequest = new QueueRequest(QueueRequestReason.PARTY_JOIN, QueueRequestParameters.create().add(partyJoinParameter).build());
                                joinable.getJoinTogetherPlayers().forEach(uuid -> Controller.getPlayer(uuid).ifPresent(controllerPlayer -> controllerPlayer.queue(partyRequest)));
                            }
                        }
                    }

                } catch (InterruptedException | ExecutionException e) {
                    // Very unfortunate, we cannot retrieve it.
                    event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.translatable("lane.controller.error.queue.kicked.none"))); // TODO CHANGE MESSAGE!!!!!!
                }
            } while(nextStage);
        }, (message) -> {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(message));
            // TODO Maybe we should keep the player at the lobby?, or register the player back
        });
    }

    @Subscribe
    public void onSettingsChanged(PlayerSettingsChangedEvent event) {
        Locale changedLocale = event.getPlayerSettings().getLocale();
        Locale currentLocale = event.getPlayer().getEffectiveLocale();
        if(currentLocale == null || !currentLocale.equals(changedLocale)) {
            // We need to update the locale
            if(changedLocale == null) {
                // Use the default locale if the changed one is null.
                changedLocale = Locale.of(configuration.getDefaultLocale());
            }
            Controller.getInstance().getPlayerManager().applySavedLocalePlayer(event.getPlayer().getUniqueId(), changedLocale);
        }
    }

    public static class Implementation extends Controller {

        private final ProxyServer server;

        public Implementation(ProxyServer server, Connection connection, DataManager dataManager) throws IOException {
            super(gson, connection, dataManager);
            this.server = server;
        }

        @Override
        public CompletableFuture<Void> joinServer(UUID uuid, String destination) {
            CompletableFuture<Void> future = new CompletableFuture<>();
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
                            future.complete(null);
                        } else if(result.getStatus() == ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS) {
                            future.completeExceptionally(new UnsuccessfulResultException(ResponsePacket.CONNECTION_IN_PROGRESS));
                        } else if(result.getStatus() == ConnectionRequestBuilder.Status.CONNECTION_CANCELLED) {
                            future.completeExceptionally(new UnsuccessfulResultException(ResponsePacket.CONNECTION_CANCELLED));
                        } else if(result.getStatus() == ConnectionRequestBuilder.Status.SERVER_DISCONNECTED) {
                            future.completeExceptionally(new UnsuccessfulResultException(ResponsePacket.CONNECTION_DISCONNECTED));
                        } else {
                            future.completeExceptionally(new UnsuccessfulResultException(ResponsePacket.UNKNOWN));
                        }
                    });
                }, () -> future.completeExceptionally(new UnsuccessfulResultException(ResponsePacket.INVALID_ID)));
            }, () -> future.completeExceptionally(new UnsuccessfulResultException(ResponsePacket.INVALID_PLAYER)));
            return future;
        }

        /*public static MapLaneMessage translator = new MapLaneMessage(Map.ofEntries( TODO
                Map.entry(Locale.ENGLISH.toLanguageTag(), Map.ofEntries(
                        Map.entry("cannotReachController", "Cannot reach controller"),
                        Map.entry("failedToRegister", "Failed to register"),
                        Map.entry("notRegisteredPlayer", "Not registered player"),
                        Map.entry("cannotFindFreeInstance", "Cannot find free instance"),
                        Map.entry("disconnect", "Disconnecting")))));*/


        @Override
        public <E extends ControllerEvent> CompletableFuture<E> handleControllerEvent(E event) {
            return server.getEventManager().fire(event);
        }

        @Override
        public void sendMessage(UUID player, Component message) {
            server.getPlayer(player).ifPresent(p -> p.sendMessage(message));
        }

        @Override
        public void disconnectPlayer(UUID player, Component message) {
            server.getPlayer(player).ifPresent(p -> p.disconnect(message));
        }

        @Override
        public void setEffectiveLocale(UUID player, Locale locale) {
            server.getPlayer(player).ifPresent(p -> p.setEffectiveLocale(locale));
        }

        @Override
        public Locale getEffectiveLocale(UUID player) {
            return server.getPlayer(player).map(Player::getEffectiveLocale).orElse(null);
        }
    }

}
