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
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class LaneInstanceSpigot extends JavaPlugin {

	// TODO Make all customizable:
	public static final boolean socketConnection = true;
	public static final String id = "survival";
	public static final String ip = "mc.slux.cz";
	public static final int port = 776;
	public static final Gson gson = new GsonBuilder().create();

	private Implementation implementation;

	@Override
	public void onEnable() {
		Connection connection;
		if(socketConnection) {
			connection = new ClientSocketConnection(id, ip, port, gson);
		}
		try {
			implementation = new Implementation(connection);
		} catch (IOException e) {
			throw new RuntimeException(e); // TODO What now?
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {

	}

	public static class Implementation extends LaneInstance {

		public Implementation(Connection connection) throws IOException {
			super(connection);
		}

	}

}
