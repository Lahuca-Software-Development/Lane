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
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.socket.client.ClientSocketConnection;
import com.lahuca.laneinstance.InstanceInstantiationException;
import com.lahuca.laneinstance.LaneInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class LaneInstanceSpigot extends JavaPlugin implements Listener {

    //    public static final boolean socketConnection = true;
//    public static final String id = "survival";
//    public static final String ip = "mc.slux.cz";
//    public static final int port = 776;
    public static final Gson gson = new GsonBuilder().create();
//    public static final boolean joinable = true;
//    public static final boolean nonPlayable = false;

    // TODO onDisable

    @Override
    public void onEnable() {
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
        Connection connection = new ClientSocketConnection("Lobby", "localhost", 7766, gson);
        boolean joinable = true;
        boolean nonPlayable = true;
        try {
            new Implementation(connection, joinable, nonPlayable); // TODO We should not be able to instantiate, as this will create multiple
        } catch(IOException e) {
            e.printStackTrace(); // TODO Send message with exception
            getPluginLoader().disablePlugin(this);
        } catch(InstanceInstantiationException e) {
            LaneInstance.getInstance().setJoinable(joinable);
            LaneInstance.getInstance().setNonPlayable(nonPlayable);
        }
    }

    public Optional<LaneInstance> impl() {
        return Optional.ofNullable(LaneInstance.getInstance());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        impl().ifPresent(impl -> impl.joinInstance(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        impl().ifPresent(impl -> impl.quitInstance(event.getPlayer().getUniqueId()));
    }

    public class Implementation extends LaneInstance {

        public Implementation(Connection connection, boolean joinable, boolean nonPlayable) throws IOException, InstanceInstantiationException {
            super(connection, joinable, nonPlayable);
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
            Player p = LaneInstanceSpigot.this.getServer().getPlayer(player);
            if(p != null && p.isOnline()) p.kickPlayer(message);
        }
    }

}
