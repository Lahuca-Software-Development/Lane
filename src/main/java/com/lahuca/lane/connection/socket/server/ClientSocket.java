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
import com.lahuca.lane.connection.ConnectionTransfer;
import com.lahuca.lane.connection.InputPacket;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.RawPacket;
import com.lahuca.lane.connection.packet.connection.ConnectionKeepAlivePacket;
import com.lahuca.lane.connection.packet.connection.ConnectionKeepAliveResultPacket;
import com.lahuca.lane.connection.packet.connection.ConnectionPacket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // TODO Store
		scheduler.scheduleWithFixedDelay(this::handleKeepAlive, 10, 10, TimeUnit.SECONDS); // TODO Configurate the seconds!
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

	private int failedKeepAlives = 0;

	/**
	 * Sends a keep alive packet to the connected client.
	 * After a certain number of packets have not been responded to, the connection is blocked.
	 */
	private void handleKeepAlive() {

	}

	private void readInput(String line) {
		System.out.println("Got: " + line);
		// TODO Add cryptography
		ConnectionTransfer transfer = gson.fromJson(line, ConnectionTransfer.class);
		Packet.getPacket(transfer.typeId()).ifPresentOrElse(packetClass -> {
			// Known packet type received.
			Packet packet = gson.fromJson(transfer.data(), packetClass);
			if(transfer.to() != null) {
				// This packet should not reach the controller, but a different client.
				connection.sendPacket(packet, transfer.to());
				return;
			}
			InputPacket iPacket = new InputPacket(packet, transfer.from(), System.currentTimeMillis(), transfer.sentAt());
			if(packet instanceof ConnectionPacket) {
				readConnectionPacket(iPacket);
				return;
			}
			input.accept(iPacket);
		}, () -> {
			// Unknown packet type received
			RawPacket rawPacket = new RawPacket(transfer.typeId(), transfer.data());
			if(transfer.to() != null) {
				// This packet should not reach the controller, but a different client.
				connection.sendPacket(rawPacket, transfer.to());
				return;
			}
			input.accept(new InputPacket(rawPacket, transfer.from(), System.currentTimeMillis(), transfer.sentAt()));
		});
	}

	/**
	 * Handle connection packets.
	 * @param inputPacket The input packet
	 */
	private void readConnectionPacket(InputPacket inputPacket) {
		Packet iPacket = inputPacket.packet();
		// TODO Use switch states for this! JAVA 21: 1.20.5 MC and above
		if(iPacket instanceof ConnectionKeepAlivePacket packet) {
			// Send packet back immediately.
			sendPacket(ConnectionKeepAliveResultPacket.ok(packet.requestId()));
		}
	}

	/**
	 * Sends a packet.
	 * @param packet the packet to send.
	 */
	public void sendPacket(Packet packet) {
		if(id == null) return; // TODO Wait for id announcement first
		String packetString = gson.toJson(packet);
		System.out.println("Send to " + id + ": " + packetString);
		ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, null,
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
