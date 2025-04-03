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
package com.lahuca.laneinstancespigot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lahuca.lane.connection.ReconnectConnection;
import com.lahuca.lane.connection.socket.client.ClientSocketConnection;
import com.lahuca.laneinstance.InstanceInstantiationException;
import com.lahuca.laneinstance.LaneInstance;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Base point for Lane Instances running on Spigot.
 * This provides the base LaneInstance code with the required code that is platform specific.
 * To retrieve a Lane Instance object use {@link #get()} or {@link #implementation()}.
 */
public class LaneInstanceSpigot extends JavaPlugin implements Listener {

    //    public static final boolean socketConnection = true;
//    public static final String id = "survival";
//    public static final String ip = "mc.slux.cz";
//    public static final int port = 776;
    public static LaneInstanceSpigot instance;
    public static final Gson gson = new GsonBuilder().create();
//    public static final boolean joinable = true;
//    public static final boolean nonPlayable = false;

    // TODO onDisable

    @Override
    public void onEnable() {
        instance = this;
        // TODO Current status: Try rejoining!
        // TODO Maaybe response is not clearing!
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        /*FileConfiguration configuration = getConfig();

        Connection connection = null;
        if(configuration.getBoolean("socketConnection")) {
            connection = new ClientSocketConnection(configuration.getString("id"), configuration.getString("ip"),
                    configuration.getInt("port"), gson);
        }

        boolean joinable = configuration.getBoolean("joinable"); TODO Undo
        boolean nonPlayable = configuration.getBoolean("nonPlayable");*/
        String id = "Lobby";
        boolean useSSL = false;
        Runnable onClose = () -> {
            getServer().getOnlinePlayers().forEach(player -> {
                if(getServer().isPrimaryThread()) {
                    player.kickPlayer("Connection closed between Instance and Controller");
                } else if(isEnabled()) {
                    getServer().getScheduler().runTask(this, () -> player.kickPlayer("Connection closed between Instance and Controller"));
                }
            });
        };
        Runnable onFinalClose = () -> {
            onClose.run();
            // More work
        };
        // TODO Maybe onReconnect?
        ReconnectConnection connection = new ClientSocketConnection(id, "localhost", 7766, gson, useSSL, onClose, onFinalClose);
        String type = "Lobby";
        boolean joinable = true;
        boolean nonPlayable = true;
        try {
            new Implementation(id, connection, type, joinable, nonPlayable);
        } catch(IOException e) {
            e.printStackTrace(); // TODO Send message with exception
            getPluginLoader().disablePlugin(this);
        } catch(InstanceInstantiationException e) {
            LaneInstance.getInstance().setJoinable(joinable);
            LaneInstance.getInstance().setNonPlayable(nonPlayable);
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
        implementation().ifPresent(impl -> impl.joinInstance(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        implementation().ifPresent(impl -> impl.quitInstance(event.getPlayer().getUniqueId()));
    }

    private class Implementation extends LaneInstance {

        private Implementation(String id, ReconnectConnection connection, String type, boolean joinable, boolean nonPlayable) throws IOException, InstanceInstantiationException {
            super(id, connection, type, joinable, nonPlayable);
        }

        private Server getServer() {
            return LaneInstanceSpigot.this.getServer();
        }

        @Override
        public int getCurrentPlayers() {
            return LaneInstanceSpigot.this.getServer().getOnlinePlayers().size();
        }

        @Override
        public int getMaxPlayers() {
            return LaneInstanceSpigot.this.getServer().getMaxPlayers();
        }

        @Override
        public void disconnectPlayer(UUID player, String message) {
            Player p = getServer().getPlayer(player);
            if(p != null && p.isOnline()) {
                if(getServer().isPrimaryThread()) {
                    p.kickPlayer(message);
                } else if(isEnabled()) {
                    getServer().getScheduler().runTask(LaneInstanceSpigot.this, () -> p.kickPlayer(message));
                }
            }
        }
    }

    public static LaneInstanceSpigot getInstance() {
        return instance;
    }
}
