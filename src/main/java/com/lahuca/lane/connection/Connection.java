/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 17-3-2024 at 14:44 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection;

import com.lahuca.lane.connection.request.Request;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.Result;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contains the base class for connections.
 */
public interface Connection {

	/**
	 * Initialise the connection by providing a consumer that handles incoming packets.
	 * @param input The packet handler.
	 * @throws IOException The exception that may occur during setup of the connection.
	 */
	void initialise(Consumer<InputPacket> input) throws IOException;

	/**
	 * Closes the connection by sending a close packet and closing the connection.
	 */
	void close();

	/**
	 * Send a packet over the connection identified by the given destination.
	 * @param packet The packet to send.
	 * @param destination The destination of the packet, null meaning the controller.
	 */
	void sendPacket(Packet packet, String destination);

	/**
	 * Sends a request packet to the given destination, and it handles the response.
	 * A request ID is generated that is being used to construct the request packet.
	 * The request is saved and forwarded to its destination.
	 * The future in the request retrieves the response, by default it timeouts after 1 second.
	 * Any generic results are cast by default.
	 * @param packetConstruction the function that created a packet based upon the request ID.
	 * @return the request with the future and request ID bundled within it.
	 * @param <T> the type of the expected result.
	 */
	<T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination);

	/**
	 * Sends a request packet to the given destination, and it handles the response.
	 * A request ID is generated that is being used to construct the request packet.
	 * The request is saved and forwarded to its destination.
	 * The future in the request retrieves the response, by default it timeouts after 1 second.
	 * Any generic results are cast by default.
	 * @param packetConstruction the function that created a packet based upon the request ID.
	 * @param timeoutSeconds the number of seconds to wait for the response.
	 * @return the request with the future and request ID bundled within it.
	 * @param <T> the type of the expected result.
	 */
	<T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, int timeoutSeconds);

	/**
	 * Sends a request packet to the given destination, and it handles the response.
	 * A request ID is generated that is being used to construct the request packet.
	 * The request is saved and forwarded to its destination.
	 * The future in the request retrieves the response, by default it timeouts after 1 second.
	 * Any generic results are cast by default.
	 * @param packetConstruction the function that created a packet based upon the request ID.
	 * @param resultParser the generic to specific result parser.
	 * @return the request with the future and request ID bundled within it.
	 * @param <T> the type of the expected result.
	 */
	<T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Result<?>, Result<T>> resultParser);

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
	 * @param <T> the type of the expected result.
	 */
	<T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Result<?>, Result<T>> resultParser, int timeoutSeconds);

	/**
	 * Sends the retrieved result into the requests' future.
	 * @param requestId The ID of the request.
	 * @param result The retrieved result.
	 * @return True whether a request with this ID exists.
	 * Or {@code true} if this invocation caused the CompletableFuture
	 * to transition to a completed state, else {@code false}.
	 */
	boolean retrieveResponse(long requestId, Result<?> result);

	/**
	 * Returns whether this connection is connected.
	 * @return True if is it connected.
	 */
	boolean isConnected();

}
