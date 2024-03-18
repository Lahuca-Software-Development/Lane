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
package com.lahuca.lane.connection.socket.client;

import com.google.gson.Gson;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.InputPacket;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.socket.SocketConnectPacket;
import com.lahuca.lane.connection.socket.SocketTransfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientSocketConnection implements Connection {

	private final String id;
	private final String ip;
	private final int port;
	private Socket socket = null;
	private Consumer<InputPacket> input = null;
	private final Gson gson;
	private PrintWriter out;
	private BufferedReader in;

	public ClientSocketConnection(String id, String ip, int port, Gson gson) {
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.gson = gson;
	}

	@Override
	public void initialise(Consumer<InputPacket> input) throws IOException {
		this.input = input;
		socket = new Socket(ip, port);
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		new Thread(this::listenForInput).start(); // TODO Maybe store thread somewhere?
		sendPacket(new SocketConnectPacket(id), null);
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
		// TODO Add cryptography
		SocketTransfer transfer = gson.fromJson(line, SocketTransfer.class);
		if(!transfer.to().equals(id)) return; // TODO Not meant for the client? Strange
		Packet.getPacket(transfer.typeId()).ifPresent(packetClass -> {
			Packet packet = gson.fromJson(transfer.data(), packetClass);
			input.accept(new InputPacket(packet, transfer.from(), System.currentTimeMillis(), transfer.sentAt()));
		}); // TODO What if the type is not registered? Parse to unknown object? Or Parse to JSONObject?
	}

	@Override
	public void sendPacket(Packet packet, String destination) {
		if(destination != null && destination.equals(id)) {
			// TODO Sending to itself?
			return;
		}
		String packetString = gson.toJson(packet);
		SocketTransfer outputPacket = new SocketTransfer(packet.getPacketId(), packetString, id,
				destination, System.currentTimeMillis());
		// TODO Add cryptography
		out.println(gson.toJson(outputPacket));
	}

	@Override
	public void stop() throws IOException {
		if(socket == null) return;
		if(socket.isClosed() || !socket.isBound()) return;
		// TODO Send close to controller
		socket.close();
	}

}
