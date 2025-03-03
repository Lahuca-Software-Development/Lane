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
import com.lahuca.lane.connection.*;
import com.lahuca.lane.connection.packet.connection.*;
import com.lahuca.lane.connection.request.Request;
import com.lahuca.lane.connection.request.RequestHandler;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ClientSocketConnection extends RequestHandler implements Connection {

    // TODO Connection randomly closed! Retry instead!!

    private final String id;
    private final String ip;
    private final int port;
    private Socket socket = null;
    private Consumer<InputPacket> input = null;
    private final Gson gson;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private Thread readThread = null;

    // KeepAlive
    private int maximumKeepAliveFails;
    private int secondsBetweenKeepAliveChecks;
    private ScheduledFuture<?> scheduledKeepAlive;
    private int numberKeepAliveFails;

    public ClientSocketConnection(String id, String ip, int port, Gson gson) {
        this(id, ip, port, gson, 3, 10);
    }

    public ClientSocketConnection(String id, String ip, int port, Gson gson, int maximumKeepAliveFails, int secondsBetweenKeepAliveChecks) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.gson = gson;
        if(maximumKeepAliveFails <= 0) maximumKeepAliveFails = 3;
        this.maximumKeepAliveFails = maximumKeepAliveFails;
        if(secondsBetweenKeepAliveChecks <= 0) secondsBetweenKeepAliveChecks = 10;
        this.secondsBetweenKeepAliveChecks = secondsBetweenKeepAliveChecks;
    }

    @Override
    public void initialise(Consumer<InputPacket> input) throws IOException {
        this.input = input;
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        readThread = new Thread(this::listenForInput);
        readThread.start();
        sendPacket(new ConnectionConnectPacket(id), null);
        scheduledKeepAlive = getScheduledExecutor().scheduleAtFixedRate(this::checkKeepAlive, secondsBetweenKeepAliveChecks, secondsBetweenKeepAliveChecks, TimeUnit.SECONDS);
        connected = true;
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
        } while(connected);
    }

    private void readInput(String line) {
        System.out.println("Got: " + line);
        // TODO Add cryptography
        ConnectionTransfer transfer = gson.fromJson(line, ConnectionTransfer.class);
        if(!transfer.to().equals(id)) return; // Odd, not meant for this client. Strange

        Packet.getPacket(transfer.typeId()).ifPresentOrElse(packetClass -> {
            // Known packet type received.
            Packet packet = gson.fromJson(transfer.data(), packetClass);
            InputPacket iPacket = new InputPacket(packet, transfer.from(), System.currentTimeMillis(), transfer.sentAt());
            if(packet instanceof ConnectionPacket) {
                readConnectionPacket(iPacket);
                return;
            }
            input.accept(iPacket);
        }, () -> {
            // Unknown packet type received
            RawPacket rawPacket = new RawPacket(transfer.typeId(), transfer.data());
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
            sendPacket(ConnectionKeepAliveResultPacket.ok(packet), inputPacket.from());
        } else if(iPacket instanceof ConnectionKeepAliveResultPacket packet) {
            retrieveResponse(packet.getRequestId(), packet.transformResult());
        } else if(iPacket instanceof ConnectionClosePacket packet) {
            // We are expecting a close, close immediately.
            close();
        }
    }

    /**
     * Send a packet over the connection identified by the given destination.
     * @param packet The packet to send.
     * @param destination The destination of the packet, null meaning the controller.
     */
    @Override
    public void sendPacket(Packet packet, String destination) {
        // TODO Maybe add function to make it async?
        if(destination != null && destination.equals(id)) {
            // TODO Sending to itself?
            return;
        }
        String packetString = gson.toJson(packet);
        ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, id,
                destination, System.currentTimeMillis());
        // TODO Add cryptography
        out.println(gson.toJson(outputPacket));
        System.out.println("Send to " + destination + ": " + packetString);
    }

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
    @Override
    public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination) {
        if(id == null) return null; // TODO Wait for id announcement first, maybe exception?
        Request<T> request = request();
        RequestPacket packet = packetConstruction.apply(request.getRequestId());

        String packetString = gson.toJson(packet);
        System.out.println("Send to " + id + ": " + packetString);

        ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, null,
                destination, System.currentTimeMillis());
        // TODO Add cryptography
        out.println(gson.toJson(outputPacket));
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
     * @return the request with the future and request ID bundled within it.
     * @param <T> the type of the expected result.
     */
    @Override
    public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, int timeoutSeconds) {
        if(id == null) return null; // TODO Wait for id announcement first, maybe exception?
        Request<T> request = request(timeoutSeconds);
        RequestPacket packet = packetConstruction.apply(request.getRequestId());

        String packetString = gson.toJson(packet);
        System.out.println("Send to " + id + ": " + packetString);

        ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, null,
                destination, System.currentTimeMillis());
        // TODO Add cryptography
        out.println(gson.toJson(outputPacket));
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
     * @return the request with the future and request ID bundled within it.
     * @param <T> the type of the expected result.
     */
    @Override
    public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Result<?>, Result<T>> resultParser) {
        if(id == null) return null; // TODO Wait for id announcement first, maybe exception?
        Request<T> request = request(resultParser);
        RequestPacket packet = packetConstruction.apply(request.getRequestId());

        String packetString = gson.toJson(packet);
        System.out.println("Send to " + id + ": " + packetString);

        ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, null,
                destination, System.currentTimeMillis());
        // TODO Add cryptography
        out.println(gson.toJson(outputPacket));
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
     * @param <T> the type of the expected result.
     */
    @Override
    public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Result<?>, Result<T>> resultParser, int timeoutSeconds) {
        if(id == null) return null; // TODO Wait for id announcement first, maybe exception?
        Request<T> request = request(resultParser, timeoutSeconds);
        RequestPacket packet = packetConstruction.apply(request.getRequestId());

        String packetString = gson.toJson(packet);
        System.out.println("Send to " + id + ": " + packetString);

        ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, null,
                destination, System.currentTimeMillis());
        // TODO Add cryptography
        out.println(gson.toJson(outputPacket));
        return request;
    }

    /**
     * Sends the retrieved result into the requests' future.
     * @param requestId The ID of the request.
     * @param result The retrieved result.
     * @return True whether a request with this ID exists.
     * Or {@code true} if this invocation caused the CompletableFuture
     * to transition to a completed state, else {@code false}.
     */
    @Override
    public boolean retrieveResponse(long requestId, Result<?> result) {
        return response(requestId, result);
    }

    @Override
    public void close() {
        if(socket == null || !connected) return;
        connected = false;
        if(!socket.isClosed() && socket.isBound()) sendPacket(new ConnectionClosePacket(), null);
        scheduledKeepAlive.cancel(true);
        stopExecutor();
        if(readThread.isAlive()) readThread.interrupt();
        try {
            socket.close();
        } catch (IOException ignored) {} // TODO Is this correct?
        // TODO Maybe run some other stuff when it is done? Like kicking players
    }

    private void checkKeepAlive() {
        sendRequestPacket(id -> new ConnectionKeepAlivePacket(id, System.currentTimeMillis()), null).getFutureResult().whenComplete((result, exception) -> {
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
        this.secondsBetweenKeepAliveChecks = secondsBetweenKeepAliveChecks;
        if(scheduledKeepAlive != null && connected) {
            scheduledKeepAlive.cancel(true);
            scheduledKeepAlive = getScheduledExecutor().scheduleAtFixedRate(this::checkKeepAlive, secondsBetweenKeepAliveChecks, secondsBetweenKeepAliveChecks, TimeUnit.SECONDS);
        }
    }

}
