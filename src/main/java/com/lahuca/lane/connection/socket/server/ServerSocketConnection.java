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
import com.lahuca.lane.connection.request.*;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServerSocketConnection extends RequestHandler implements Connection {

	private final int port;
	private ServerSocket socket = null;
	private Consumer<InputPacket> input = null;
	private final Gson gson;
	private final boolean useSSL;
	private final HashMap<String, ClientSocket> clients = new HashMap<>();
	private final HashSet<ClientSocket> unassignedClients = new HashSet<>();
	private final BiFunction<String, ClientSocket, Boolean> assignId = (id, client) -> {
		if(!unassignedClients.contains(client)) return false;
		if(clients.containsKey(id)) {
			return false;
		}
		clients.put(id, client);
		unassignedClients.remove(client);
		return true;
	};
	// TODO Maybe do consumer to abstract funcgtion.
	/**
	 * This consumer is called when a client disconnects.
	 * It is provided with the ID of the client.
	 * When it had not announced the ID yet, this is null.
	 */
	private Consumer<String> onClientRemove = null;

	private Thread listenThread = null;
	private boolean started = false;

	public ServerSocketConnection(int port, Gson gson, boolean useSSL) {
		this.port = port;
		this.gson = gson;
		this.useSSL = useSSL;
	}

	@Override
	public void initialise(Consumer<InputPacket> input) throws IOException {
		this.input = input;
		started = true;
		if(useSSL) {
			SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			socket = factory.createServerSocket(port);
		} else {
			socket = new ServerSocket(port);
		}
		listenThread = new Thread(this::listenForClients);
		listenThread.start();
	}

	private void listenForClients() {
		Consumer<ClientSocket> onClose = (client) -> {
			client.getId().ifPresentOrElse(clients::remove, () -> unassignedClients.remove(client));
			onClientRemove.accept(client.getId().orElse(null));
		};
		while(isConnected() && started) {
			try {
				Socket client = socket.accept();
				unassignedClients.add(new ClientSocket(this, client, input, gson, assignId, onClose));
			} catch (IOException e) {
				// Well, looks like server is down, or has to stop.
				close();
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
		if(destination == null || !isConnected()) {
			return;
		}
		ClientSocket client = clients.get(destination);
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
		if(!isConnected()) return null; // TODO Return differently
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
		if(!isConnected()) return null; // TODO Return differently
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
	 * @param resultParser the generic-to-specific result parser.
	 * @return the request with the future and request ID bundled within it. Null if there is no client with the given destination found.
	 * @param <T> the type of the expected result.
	 */
	@Override
	public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Object, T> resultParser) {
		if(!isConnected()) return null; // TODO Return differently
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
	 * The future in the request retrieves the response, by default, it timeouts after 3 seconds.
	 * Any generic results are cast by default.
	 * @param packetConstruction the function that created a packet based upon the request ID.
	 * @param resultParser the generic-to-specific result parser.
	 * @param timeoutSeconds the number of seconds to wait for the response.
	 * @return the request with the future and request ID bundled within it.
	 * Null if there is no client with the given destination found.
	 * @param <T> the type of the expected result.
	 */
	@Override
	public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Object, T> resultParser, int timeoutSeconds) {
		if(!isConnected()) return null; // TODO Return differently
		ClientSocket client = clients.get(destination);
		if(client == null || !client.isConnected()) return null; // TODO Handle if client is closed!
		Request<T> request = request(resultParser, timeoutSeconds);
		RequestPacket packet = packetConstruction.apply(request.getRequestId());
		client.sendPacket(packet);
		return request;
	}

	@Override
	public <T extends ResponsePacket<Object>> boolean retrieveResponse(long requestId, T response) {
		return response(requestId, response);
	}

	@Override
	public boolean isConnected() {
		return socket != null && socket.isBound() && !socket.isClosed();
	}

	@Override
	public void close() {
		if (listenThread != null && listenThread.isAlive()) listenThread.interrupt();
		stopExecutor();
		listenThread = null;
		new HashSet<>(clients.values()).forEach(ClientSocket::close);
		new HashSet<>(unassignedClients).forEach(ClientSocket::close);
		clients.clear();
		unassignedClients.clear();
		try {
			if (socket != null) socket.close();
		} catch (IOException ignored) {
		} finally {
			started = false;
		}
		// TODO Maybe run some other stuff when it is done? Like kicking players
	}

	public void setOnClientRemove(Consumer<String> onClientRemove) {
		this.onClientRemove = onClientRemove;
	}

}
