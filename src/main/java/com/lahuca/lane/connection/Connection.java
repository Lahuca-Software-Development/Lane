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
import com.lahuca.lane.connection.request.ResponsePacket;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contains the base class for connections.
 */
public interface Connection {

    /**
     * Initialise the connection by providing a consumer that handles incoming packets.
     *
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
     *
     * @param packet      The packet to send.
     * @param destination The destination of the packet, null meaning the controller.
     */
    void sendPacket(Packet packet, String destination);

    /**
     * Send a packet over the connection identified by the given destinations.
     *
     * @param destinations The destinations of the packet, null value means the controller.
     * @param packet       The packet to send.
     */
    default void sendPacket(Set<String> destinations, Packet packet) {
        Objects.requireNonNull(destinations, "destinations cannot be null");
        destinations.forEach(destination -> sendPacket(packet, destination));
    }

    /**
     * Sends a request packet to the given destination, and it handles the response.
     * A request ID is generated that is being used to construct the request packet.
     * The request is saved and forwarded to its destination.
     * The future in the request retrieves the response, by default, it timeouts after 3 seconds.
     * Any generic results are cast by default.
     *
     * @param packetConstruction the function that created a packet based upon the request ID.
     * @param <T>                the type of the expected result.
     * @return the request with the future and request ID bundled within it.
     */
    <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination);

    /**
     * Sends a request packet to the given destination, and it handles the response.
     * A request ID is generated that is being used to construct the request packet.
     * The request is saved and forwarded to its destination.
     * The future in the request retrieves the response, by default, it timeouts after 3 seconds.
     * Any generic results are cast by default.
     *
     * @param packetConstruction the function that created a packet based upon the request ID.
     * @param timeoutSeconds     the number of seconds to wait for the response.
     * @param <T>                the type of the expected result.
     * @return the request with the future and request ID bundled within it.
     */
    <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, int timeoutSeconds);

    /**
     * Sends a request packet to the given destination, and it handles the response.
     * A request ID is generated that is being used to construct the request packet.
     * The request is saved and forwarded to its destination.
     * The future in the request retrieves the response, by default, it timeouts after 3 seconds.
     * Any generic results are cast by default.
     *
     * @param packetConstruction the function that created a packet based upon the request ID.
     * @param resultParser       the generic-to-specific result parser.
     * @param <T>                the type of the expected result.
     * @return the request with the future and request ID bundled within it.
     */
    <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Object, T> resultParser);

    /**
     * Sends a request packet to the given destination, and it handles the response.
     * A request ID is generated that is being used to construct the request packet.
     * The request is saved and forwarded to its destination.
     * The future in the request retrieves the response, by default, it timeouts after 3 seconds.
     * Any generic results are cast by default.
     *
     * @param packetConstruction the function that created a packet based upon the request ID.
     * @param resultParser       the generic-to-specific result parser.
     * @param timeoutSeconds     the number of seconds to wait for the response.
     * @param <T>                the type of the expected result.
     * @return the request with the future and request ID bundled within it.
     */
    <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Object, T> resultParser, int timeoutSeconds);

    /**
     * Sends the retrieved response into the requests' future.
     *
     * @param requestId The ID of the request.
     * @param response  The retrieved response.
     * @param <T>       the response packet type
     * @return True whether a request with this ID exists.
     * Or {@code true} if this invocation caused the CompletableFuture
     * to transition to a completed state, else {@code false}.
     */
    <T extends ResponsePacket<Object>> boolean retrieveResponse(long requestId, T response);

    /**
     * Returns whether this connection is connected.
     *
     * @return True if it is connected.
     */
    boolean isConnected();

}
