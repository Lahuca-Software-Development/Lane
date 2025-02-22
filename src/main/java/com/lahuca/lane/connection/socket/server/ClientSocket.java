/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 17-3-2024 at 17:39 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection.socket.server;

import com.google.gson.Gson;
import com.lahuca.lane.connection.InputPacket;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.socket.SocketConnectPacket;
import com.lahuca.lane.connection.socket.SocketTransfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientSocket {

	private final ServerSocketConnection connection;
	private final Socket socket;
	private final PrintWriter out;
	private final BufferedReader in;
	private final Consumer<InputPacket> input;
	private final Gson gson;
	private String id = null;
	private String type = null;
	private final BiConsumer<String, ClientSocket> assignId;

	public ClientSocket(ServerSocketConnection connection, Socket socket, Consumer<InputPacket> input,
						Gson gson, BiConsumer<String, ClientSocket> assignId) throws IOException {
		this.connection = connection;
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.assignId = assignId;
		this.socket = socket;
		this.input = input;
		this.gson = gson;
		new Thread(this::listenForInput).start(); // TODO Store thread?
	}

	private void listenForInput() {
		String inputLine;
		do {
			try {
				inputLine = in.readLine();
				if(inputLine != null) readInput(inputLine);
			} catch (IOException e) {
				throw new RuntimeException(e); // TODO What now?
			}
		} while(inputLine != null);
	}

	private void readInput(String line) {
		System.out.println("Got: " + line);
		// TODO Add cryptography
		SocketTransfer transfer = gson.fromJson(line, SocketTransfer.class);
		Packet.getPacket(transfer.typeId()).ifPresent(packetClass -> {
			Packet packet = gson.fromJson(transfer.data(), packetClass);
			if(transfer.to() != null) {
				// This packet should not reach the controller, but a different client.
				connection.sendPacket(packet, transfer.to());
			} else {
				if(packet instanceof SocketConnectPacket socketConnect) {
					id = socketConnect.clientId();
					assignId.accept(id, this);
				} else {
					input.accept(new InputPacket(packet, transfer.from(), System.currentTimeMillis(), transfer.sentAt()));
				}
			}
		}); // TODO What if the type is not registered? Parse to unknown object? Or Parse to JSONObject?
	}

	public void sendPacket(Packet packet) {
		if(id == null) return; // TODO Wait for id announcement first
		String packetString = gson.toJson(packet);
		System.out.println("Send to " + id + ": " + packetString);
		SocketTransfer outputPacket = new SocketTransfer(packet.getPacketId(), packetString, null,
				id, System.currentTimeMillis());
		// TODO Add cryptography
		out.println(gson.toJson(outputPacket));
	}

	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			// TODO What should happen?
			throw new RuntimeException(e);
		}
	}

	public String getType() {
		return type;
	}

}
