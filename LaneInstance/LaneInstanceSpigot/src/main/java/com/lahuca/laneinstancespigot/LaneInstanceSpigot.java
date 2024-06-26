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
import com.lahuca.laneinstance.LaneInstance;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Optional;

public class LaneInstanceSpigot extends JavaPlugin implements Listener {

    // TODO Make all customizable:
    public static final boolean socketConnection = true;
    public static final String id = "survival";
    public static final String ip = "mc.slux.cz";
    public static final int port = 776;
    public static final Gson gson = new GsonBuilder().create();
    public static final boolean joinable = true;
    public static final boolean nonPlayable = false;

    private Implementation implementation;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        Connection connection;
        if(socketConnection) {
            connection = new ClientSocketConnection(id, ip, port, gson);
        }

        try {
            implementation = new Implementation(connection, joinable, nonPlayable);
        } catch(IOException e) {
            e.printStackTrace(); // TODO Send message with exception
            getPluginLoader().disablePlugin(this);
        }
    }

    public Optional<Implementation> impl() {
        return Optional.ofNullable(implementation);
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

        public Implementation(Connection connection, boolean joinable, boolean nonPlayable) throws IOException {
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

    }

}
