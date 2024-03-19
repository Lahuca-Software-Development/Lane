/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 17-3-2024 at 14:46 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection.socket.server;

import com.google.gson.Gson;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.InputPacket;
import com.lahuca.lane.connection.Packet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ServerSocketConnection implements Connection {

	private final int port;
	private ServerSocket socket = null;
	private Consumer<InputPacket> input = null;
	private final Gson gson;
	private final HashMap<String, ClientSocket> clients = new HashMap<>();
	private final HashSet<ClientSocket> unassignedClients = new HashSet<>();
	private final BiConsumer<String, ClientSocket> assignId = (id, client) -> {
		if(!unassignedClients.contains(client)) return;
		clients.put(id, client);
		unassignedClients.remove(client);
	};

	public ServerSocketConnection(int port, Gson gson) {
		this.port = port;
		this.gson = gson;
	}

	@Override
	public void initialise(Consumer<InputPacket> input) throws IOException {
		this.input = input;
		socket = new ServerSocket(port);
		new Thread(this::listenForClients).start(); // TODO Maybe store thread somewhere?
	}

	private void listenForClients() {
		while(socket != null && !socket.isClosed() && socket.isBound()) {
			try {
				Socket client = socket.accept();
				unassignedClients.add(new ClientSocket(this, client, input, gson, assignId));
			} catch (IOException e) {
				// TODO What to do? Couldn't open?
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void sendPacket(Packet packet, String destination) {
		if(destination == null) {
			// TODO Sending to controller? Controller to controller?
			return;
		}

		ClientSocket client = clients.get(destination);
		if(client != null) client.sendPacket(packet);
	}

	@Override
	public void stop() throws IOException {
		if(socket == null) return;
		if(socket.isClosed() || !socket.isBound()) return;
		// TODO Send close to clients
		clients.values().forEach(ClientSocket::close);
		unassignedClients.forEach(ClientSocket::close);
		socket.close();
	}

}
