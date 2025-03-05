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
import com.lahuca.lane.connection.packet.connection.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ScheduledFuture;
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
	private final BiConsumer<String, ClientSocket> assignId;
	private boolean started = false;
	private Thread readThread = null;

	// KeepAlive
	private int maximumKeepAliveFails;
	private ScheduledFuture<?> scheduledKeepAlive;
	private int numberKeepAliveFails;

	public ClientSocket(ServerSocketConnection connection, Socket socket, Consumer<InputPacket> input,
						Gson gson, BiConsumer<String, ClientSocket> assignId) throws IOException {
        this(connection, socket, input, gson, assignId, 3, 10);
	}

	public ClientSocket(ServerSocketConnection connection, Socket socket, Consumer<InputPacket> input,
						Gson gson, BiConsumer<String, ClientSocket> assignId, int maximumKeepAliveFails, int secondsBetweenKeepAliveChecks) throws IOException {
		this.connection = connection;
		started = true;
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.assignId = assignId;
		this.socket = socket;
		this.input = input;
		this.gson = gson;
		readThread = new Thread(this::listenForInput);
		readThread.start();
		if(maximumKeepAliveFails <= 0) maximumKeepAliveFails = 3;
		this.maximumKeepAliveFails = maximumKeepAliveFails;
		if(secondsBetweenKeepAliveChecks <= 0) secondsBetweenKeepAliveChecks = 10;
		scheduledKeepAlive = connection.getScheduledExecutor().scheduleAtFixedRate(this::checkKeepAlive, secondsBetweenKeepAliveChecks, secondsBetweenKeepAliveChecks, TimeUnit.SECONDS);
	}

	private void listenForInput() {
		String inputLine;
		do {
			try {
				inputLine = in.readLine();
				if(inputLine == null) {
					// End of stream, closed
					close();
					return;
				}
				readInput(inputLine);
			} catch(IOException e) {
				// Error while reading.
				close();
				return;
			}
		} while(isConnected());
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
		if(iPacket instanceof ConnectionConnectPacket packet) {
			id = packet.clientId();
			assignId.accept(id, this);
		} else if(iPacket instanceof ConnectionKeepAlivePacket packet) {
			// Send packet back immediately.
			sendPacket(ConnectionKeepAliveResultPacket.ok(packet));
		} else if(iPacket instanceof ConnectionKeepAliveResultPacket packet) {
			connection.retrieveResponse(packet.getRequestId(), packet.transformResult());
		} else if(iPacket instanceof ConnectionClosePacket packet) {
			// We are expecting a close, close immediately.
			close();
		}
	}

	/**
	 * Sends a packet.
	 * @param packet the packet to send.
	 */
	public void sendPacket(Packet packet) {
		if(id == null || !isConnected()) return; // TODO Wait for id announcement first
		String packetString = gson.toJson(packet);
		System.out.println("Send to " + id + ": " + packetString);
		ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, null,
				id, System.currentTimeMillis());
		// TODO Add cryptography
		out.println(gson.toJson(outputPacket));
	}

	public void close() {
		if(scheduledKeepAlive != null) scheduledKeepAlive.cancel(true);
		scheduledKeepAlive = null;
		if(readThread != null && readThread.isAlive()) readThread.interrupt();
		if(isConnected()) sendPacket(new ConnectionClosePacket());
        try {
			if(in != null) in.close();
			if(out != null) out.close();
            if(socket != null) socket.close();
        } catch (IOException e) {
        } finally {
			started = false;
		}
		// TODO Maybe run some other stuff when it is done? Like kicking players
	}

	private void checkKeepAlive() {
		connection.sendRequestPacket(requestId -> new ConnectionKeepAlivePacket(requestId, System.currentTimeMillis()), id).getFutureResult().whenComplete((result, exception) -> {
			if(exception != null || result == null || !result.isSuccessful()) {
				numberKeepAliveFails++;
				if(numberKeepAliveFails > maximumKeepAliveFails) {
					close();
				}
			} else {
				numberKeepAliveFails = 0;
			}
		});
	}

	public void setMaximumKeepAliveFails(int maximumKeepAliveFails) {
		if(maximumKeepAliveFails <= 0) return;
		this.maximumKeepAliveFails = maximumKeepAliveFails;
	}

	public void setSecondsBetweenKeepAliveChecks(int secondsBetweenKeepAliveChecks) {
		if(secondsBetweenKeepAliveChecks <= 0) return;
		if(scheduledKeepAlive != null && isConnected()) {
			scheduledKeepAlive.cancel(true);
			scheduledKeepAlive = connection.getScheduledExecutor().scheduleAtFixedRate(this::checkKeepAlive, secondsBetweenKeepAliveChecks, secondsBetweenKeepAliveChecks, TimeUnit.SECONDS);
		}
	}

	public boolean isConnected() {
		return socket != null && socket.isConnected() && socket.isBound() && !socket.isClosed();
	}

}
