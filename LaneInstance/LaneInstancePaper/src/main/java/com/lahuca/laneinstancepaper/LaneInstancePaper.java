/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 19-3-2024 at 11:22 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.laneinstancepaper;

import com.google.gson.Gson;
import com.lahuca.lane.ReconnectConnection;
import com.lahuca.lane.connection.socket.client.ClientSocketConnection;
import com.lahuca.laneinstance.InstanceInstantiationException;
import com.lahuca.laneinstance.LaneInstance;
import com.lahuca.laneinstance.events.*;
import com.lahuca.laneinstancepaper.events.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Base point for Lane Instances running on Paper.
 * This provides the base LaneInstance code with the required code that is platform specific.
 * To retrieve a Lane Instance object use {@link #get()} or {@link #implementation()}.
 */
public class LaneInstancePaper extends JavaPlugin implements Listener {

    //    public static final boolean socketConnection = true;
//    public static final String id = "survival";
//    public static final String ip = "mc.slux.cz";
//    public static final int port = 776;
    public static final Gson gson = GsonComponentSerializer.builder().editOptions(b -> b.value(JSONOptions.EMIT_HOVER_SHOW_ENTITY_ID_AS_INT_ARRAY, false)).build().serializer();
//    public static final boolean joinable = true;
//    public static final boolean nonPlayable = false;

    private LaneInstancePaper instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        FileConfiguration configuration = getConfig();


        String id = configuration.getString("id");
        Runnable onClose = () -> {
            getServer().getOnlinePlayers().forEach(player -> {
                if(getServer().isPrimaryThread()) {
                    player.kick(Component.text("Connection closed between Instance and Controller"));
                } else if(isEnabled()) {
                    getServer().getScheduler().runTask(this, () -> player.kick(Component.text("Connection closed between Instance and Controller")));
                }
            });
        };
        Runnable onFinalClose = () -> {
            onClose.run();
            // More work
        };
        // TODO Maybe onReconnect?
        ReconnectConnection connection = null;
        if(configuration.getString("connection.type", "SOCKET").equalsIgnoreCase("SOCKET")) {
            connection = new ClientSocketConnection(id, configuration.getString("connection.ip"),
                    configuration.getInt("connection.port"), gson, configuration.getBoolean("connection.socket.ssl"), onClose, onFinalClose);
        }

        String type = configuration.getString("type");
        boolean onlineJoinable = configuration.getBoolean("onlineJoinable");
        boolean playersJoinable = configuration.getBoolean("playersJoinable");
        boolean playingJoinable = configuration.getBoolean("playingJoinable");
        int maxOnlineSlots = configuration.getInt("maxOnlineSlots");
        int maxPlayersSlots = configuration.getInt("maxPlayersSlots");
        int maxPlayingSlots = configuration.getInt("maxPlayingSlots");
        boolean onlineKickable = configuration.getBoolean("onlineKickable");
        boolean playersKickable = configuration.getBoolean("playersKickable");
        boolean playingKickable = configuration.getBoolean("playingKickable");
        boolean isPrivate = configuration.getBoolean("isPrivate");
        try {
            new Implementation(id, connection, type, onlineJoinable, playersJoinable, playingJoinable, maxOnlineSlots, maxPlayersSlots, maxPlayingSlots, onlineKickable, playersKickable, playingKickable, isPrivate);
        } catch(IOException e) {
            e.printStackTrace(); // TODO Send message with exception
            getServer().getPluginManager().disablePlugin(this);
        } catch(InstanceInstantiationException e) {
            LaneInstance.getInstance().getPlayerManager().updateJoinableSlots(onlineJoinable, playersJoinable,
                    playingJoinable, maxOnlineSlots, maxPlayersSlots, maxPlayingSlots,
                    onlineKickable, playersKickable, playingKickable, isPrivate);
        }
    }


    @Override
    public void onDisable() {
        implementation().ifPresent(LaneInstance::shutdown);
    }

    public static Optional<LaneInstance> impl() {
        return implementation();
    }

    public static Optional<LaneInstance> implementation() {
        return Optional.ofNullable(LaneInstance.getInstance());
    }

    public static Optional<LaneInstance> get() {
        return implementation();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        implementation().ifPresent(impl -> impl.getPlayerManager().joinInstance(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        implementation().ifPresent(impl -> impl.getPlayerManager().quitInstance(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        // TODO! Paper has no solutions yet
    }

    private class Implementation extends LaneInstance {

        private Implementation(String id, ReconnectConnection connection, String type, boolean onlineJoinable, boolean playersJoinable, boolean playingJoinable, int maxOnlineSlots, int maxPlayersSlots, int maxPlayingSlots, boolean onlineKickable, boolean playersKickable, boolean playingKickable, boolean isPrivate) throws IOException, InstanceInstantiationException {
            super(id, connection, type, onlineJoinable, playersJoinable, playingJoinable, maxOnlineSlots, maxPlayersSlots, maxPlayingSlots, onlineKickable, playersKickable, playingKickable, isPrivate);
        }

        private Server getServer() {
            return LaneInstancePaper.this.getServer();
        }

        @Override
        public void disconnectPlayer(UUID player, Component message) {
            Player p = getServer().getPlayer(player);
            if(p != null && p.isOnline()) {
                if(getServer().isPrimaryThread()) {
                    p.kick(message);
                } else if(isEnabled()) {
                    getServer().getScheduler().runTask(LaneInstancePaper.this, () -> p.kick(message));
                }
            }
        }

        @Override
        public <E extends InstanceEvent> CompletableFuture<E> handleInstanceEvent(E event) {
            // Construct the paper event
            PaperInstanceEvent<?> paperEvent = switch (event) {
                case InstanceJoinEvent obj -> new PaperInstanceJoinEvent(obj);
                case InstanceJoinGameEvent obj -> new PaperInstanceJoinGameEvent(obj);
                case InstanceQuitEvent obj -> new PaperInstanceQuitEvent(obj);
                case InstanceQuitGameEvent obj -> new PaperInstanceQuitGameEvent(obj);
                case InstanceSwitchGameQueueTypeEvent obj -> new PaperInstanceSwitchGameQueueTypeEvent(obj);
                case InstanceSwitchQueueTypeEvent obj -> new PaperInstanceSwitchQueueTypeEvent(obj);
                case InstanceStartupGameEvent obj -> new PaperInstanceStartupGameEvent(obj);
                case InstanceShutdownGameEvent obj -> new PaperInstanceShutdownGameEvent(obj);
                default -> new PaperInstanceGenericEvent(event);
            };
            // Call the event, this will also update our parameter here
            paperEvent.callEvent();
            return CompletableFuture.completedFuture(event);
        }

        @Override
        public void runOnMainThread(Runnable runnable) {
            getServer().getScheduler().runTask(instance, runnable);
        }
    }

}
