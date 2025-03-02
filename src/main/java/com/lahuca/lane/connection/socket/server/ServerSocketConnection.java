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
import com.lahuca.lane.connection.packet.connection.ConnectionClosePacket;
import com.lahuca.lane.connection.request.Request;
import com.lahuca.lane.connection.request.RequestHandler;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.Result;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServerSocketConnection extends RequestHandler implements Connection {

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
				System.out.println("Unassigned: " + unassignedClients);
			} catch (IOException e) {
				// TODO What to do? Couldn't open?
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Send a packet over the connection identified by the given destination.
	 * @param packet The packet to send.
	 * @param destination The destination of the packet, null meaning the controller.
	 */
	@Override
	public void sendPacket(Packet packet, String destination) {
		if(destination == null) {
			// TODO Sending to controller? Controller to controller?
			return;
		}

		ClientSocket client = clients.get(destination);
		System.out.println("Clients: " + clients + ", " + destination + " " + client);
		if(client != null) client.sendPacket(packet);
	}

	/**
	 * Sends a request packet to the given destination, and it handles the response.
	 * A request ID is generated that is being used to construct the request packet.
	 * The request is saved and forwarded to its destination.
	 * The future in the request retrieves the response, by default it timeouts after 1 second.
	 * Any generic results are cast by default.
	 * @param packetConstruction the function that created a packet based upon the request ID.
	 * @return the request with the future and request ID bundled within it. Null if there is no client with the given destination found.
	 * @param <T> the type of the expected result.
	 */
	@Override
	public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination) {
		ClientSocket client = clients.get(destination);
		if(client == null) return null;
		Request<T> request = request();
		RequestPacket packet = packetConstruction.apply(request.getRequestId());
		client.sendPacket(packet);
		return request;
	}

	/**
	 * Sends a request packet to the given destination, and it handles the response.
	 * A request ID is generated that is being used to construct the request packet.
	 * The request is saved and forwarded to its destination.
	 * The future in the request retrieves the response, by default it timeouts after 1 second.
	 * Any generic results are cast by default.
	 * @param packetConstruction the function that created a packet based upon the request ID.
	 * @param timeoutSeconds the number of seconds to wait for the response.
	 * @return the request with the future and request ID bundled within it. Null if there is no client with the given destination found.
	 * @param <T> the type of the expected result.
	 */
	@Override
	public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, int timeoutSeconds) {
		ClientSocket client = clients.get(destination);
		if(client == null) return null;
		Request<T> request = request(timeoutSeconds);
		RequestPacket packet = packetConstruction.apply(request.getRequestId());
		client.sendPacket(packet);
		return request;
	}

	/**
	 * Sends a request packet to the given destination, and it handles the response.
	 * A request ID is generated that is being used to construct the request packet.
	 * The request is saved and forwarded to its destination.
	 * The future in the request retrieves the response, by default it timeouts after 1 second.
	 * Any generic results are cast by default.
	 * @param packetConstruction the function that created a packet based upon the request ID.
	 * @param resultParser the generic to specific result parser.
	 * @return the request with the future and request ID bundled within it. Null if there is no client with the given destination found.
	 * @param <T> the type of the expected result.
	 */
	@Override
	public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Result<?>, Result<T>> resultParser) {
		ClientSocket client = clients.get(destination);
		if(client == null) return null;
		Request<T> request = request(resultParser);
		RequestPacket packet = packetConstruction.apply(request.getRequestId());
		client.sendPacket(packet);
		return request;
	}

	/**
	 * Sends a request packet to the given destination, and it handles the response.
	 * A request ID is generated that is being used to construct the request packet.
	 * The request is saved and forwarded to its destination.
	 * The future in the request retrieves the response, by default it timeouts after 1 second.
	 * Any generic results are cast by default.
	 * @param packetConstruction the function that created a packet based upon the request ID.
	 * @param resultParser the generic to specific result parser.
	 * @param timeoutSeconds the number of seconds to wait for the response.
	 * @return the request with the future and request ID bundled within it.
	 * Null if there is no client with the given destination found.
	 * @param <T> the type of the expected result.
	 */
	@Override
	public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Result<?>, Result<T>> resultParser, int timeoutSeconds) {
		ClientSocket client = clients.get(destination);
		if(client == null) return null; // TODO Handle if client is closed!
		Request<T> request = request(resultParser, timeoutSeconds);
		RequestPacket packet = packetConstruction.apply(request.getRequestId());
		client.sendPacket(packet);
		return request;
	}

	@Override
	public boolean retrieveResponse(long requestId, Result<?> result) {
		return response(requestId, result);
	}

	@Override
	public void close() throws IOException {
		if(socket == null) return;
		if(socket.isClosed() || !socket.isBound()) return;
		ConnectionClosePacket closure = new ConnectionClosePacket();
		clients.keySet().forEach(client -> sendPacket(closure, client));
		unassignedClients.forEach(client -> client.sendPacket(closure));
		clients.values().forEach(ClientSocket::close);
		unassignedClients.forEach(ClientSocket::close);
		socket.close();
	}

}
