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
import com.lahuca.lane.ReconnectConnection;
import com.lahuca.lane.connection.ConnectionTransfer;
import com.lahuca.lane.connection.InputPacket;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.RawPacket;
import com.lahuca.lane.connection.packet.connection.*;
import com.lahuca.lane.connection.request.*;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ClientSocketConnection extends RequestHandler implements ReconnectConnection {

    private final String id;
    private final String ip;
    private final int port;
    private Socket socket = null;
    private Consumer<InputPacket> input = null;
    private final Gson gson;
    private final boolean useSSL;
    private PrintWriter out;
    private BufferedReader in;
    private Thread readThread = null;
    private boolean started = false;

    // TODO Bottom two runnables, probably to abstract methods.
    /**
     * This method is run upon when the connection has closed.
     * This is called when reconnects are tried after closure.
     */
    private final Runnable onClose;
    /**
     * This method is run when the connection is closed and reconnecting does not happen anymore.
     * This is called instead of {@link #onClose}.
     */
    private final Runnable onFinalClose;

    // Reconnect upon close
    /**
     * This value determines when the connection closes, whether it should try to reconnect.
     */
    private boolean reconnect = true;
    private int secondsBetweenReconnections;
    private Runnable onReconnect;

    // KeepAlive
    private int maximumKeepAliveFails;
    private int secondsBetweenKeepAliveChecks;
    private ScheduledFuture<?> scheduledKeepAlive;
    private int numberKeepAliveFails;

    public ClientSocketConnection(String id, String ip, int port, Gson gson, boolean useSSL) {
        this(id, ip, port, gson, useSSL, null, null, true, 60, 3, 60);
    }

    public ClientSocketConnection(String id, String ip, int port, Gson gson, boolean useSSL, Runnable onClose, Runnable onFinalClose) {
        this(id, ip, port, gson, useSSL, onClose, onFinalClose, true, 10, 3, 60); // TODO Modify reconnect time to 60
    }

    public ClientSocketConnection(String id, String ip, int port, Gson gson, boolean useSSL, Runnable onClose, Runnable onFinalClose, boolean reconnect, int secondsBetweenReconnections, int maximumKeepAliveFails, int secondsBetweenKeepAliveChecks) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.gson = gson;
        this.useSSL = useSSL;
        this.onClose = onClose;
        this.onFinalClose = onFinalClose;
        this.reconnect = reconnect;
        if(secondsBetweenReconnections <= 0) secondsBetweenReconnections = 60;
        this.secondsBetweenReconnections = secondsBetweenReconnections;
        if(maximumKeepAliveFails <= 0) maximumKeepAliveFails = 3;
        this.maximumKeepAliveFails = maximumKeepAliveFails;
        if(secondsBetweenKeepAliveChecks <= 0) secondsBetweenKeepAliveChecks = 60;
        this.secondsBetweenKeepAliveChecks = secondsBetweenKeepAliveChecks;
    }

    @Override
    public void connect() throws IOException {
        if(started || isConnected()) return;
        started = true;
        if (useSSL) {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = factory.createSocket(ip, port);
        } else {
            socket = new Socket(ip, port);
        }
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        readThread = new Thread(this::listenForInput);
        readThread.start();
        startTask();
        sendPacket(new ConnectionConnectPacket(id), null);
        scheduledKeepAlive = getScheduledExecutor().scheduleAtFixedRate(this::checkKeepAlive, secondsBetweenKeepAliveChecks, secondsBetweenKeepAliveChecks, TimeUnit.SECONDS);
    }

    @Override
    public void initialise(Consumer<InputPacket> input) throws IOException {
        this.input = input;
        connect();
    }

    private void listenForInput() {
        String inputLine;
        do {
            try {
                inputLine = in.readLine();
                if(inputLine == null) {
                    // End of stream, closed
                    closeAndReconnect();
                    return;
                }
                readInput(inputLine);
            } catch(IOException e) {
                e.printStackTrace();
                // Error while reading.
                closeAndReconnect();
                return;
            }
        } while(isConnected());
    }

    private void readInput(String line) {
        System.out.println("Got: " + line);
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
            retrieveResponse(packet.getRequestId(), packet.toObjectResponsePacket());
        } else if(iPacket instanceof ConnectionClosePacket packet) {
            // We are expecting a close, close immediately.
            closeAndReconnect();
        }
    }

    /**
     * Send a packet over the connection identified by the given destination.
     * @param packet The packet to send.
     * @param destination The destination of the packet, null meaning the controller.
     */
    @Override
    public void sendPacket(Packet packet, String destination) {
        if(!isConnected()) return;
        // TODO Maybe add function to make it async?
        if(destination != null && destination.equals(id)) {
            // TODO Sending to itself?
            return;
        }
        String packetString = gson.toJson(packet);
        ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, id,
                destination, System.currentTimeMillis());
        System.out.println("Send to " + destination + ": " + packetString);
        out.println(gson.toJson(outputPacket));
    }

    private static <T> Request<T> disconnectedRequest() {
        return new Request<>(new ResultUnsuccessfulException(ResponsePacket.CONTROLLER_DISCONNECTED));
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
        if(id == null || !isConnected()) return disconnectedRequest();
        Request<T> request = request();
        RequestPacket packet = packetConstruction.apply(request.getRequestId());

        String packetString = gson.toJson(packet);
        System.out.println("Send to " + id + ": " + packetString);

        ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, id,
                destination, System.currentTimeMillis());
        out.println(gson.toJson(outputPacket));
        return request;
    }

    /**
     * Sends a request packet to the given destination, and it handles the response.
     * A request ID is generated that is being used to construct the request packet.
     * The request is saved and forwarded to its destination.
     * The future in the request retrieves the response, by default, it timeouts after 3 seconds.
     * Any generic results are cast by default.
     * @param packetConstruction the function that created a packet based upon the request ID.
     * @param timeoutSeconds the number of seconds to wait for the response.
     * @return the request with the future and request ID bundled within it.
     * @param <T> the type of the expected result.
     */
    @Override
    public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, int timeoutSeconds) {
        if(id == null || !isConnected()) return disconnectedRequest();
        Request<T> request = request(timeoutSeconds);
        RequestPacket packet = packetConstruction.apply(request.getRequestId());

        String packetString = gson.toJson(packet);
        System.out.println("Send to " + id + ": " + packetString);

        ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, id,
                destination, System.currentTimeMillis());
        out.println(gson.toJson(outputPacket));
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
     * @return the request with the future and request ID bundled within it.
     * @param <T> the type of the expected result.
     */
    @Override
    public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Object, T> resultParser) {
        if(id == null || !isConnected()) return disconnectedRequest();
        Request<T> request = request(resultParser);
        RequestPacket packet = packetConstruction.apply(request.getRequestId());

        String packetString = gson.toJson(packet);
        System.out.println("Send to " + id + ": " + packetString);

        ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, id,
                destination, System.currentTimeMillis());
        out.println(gson.toJson(outputPacket));
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
     * @param <T> the type of the expected result.
     */
    @Override
    public <T> Request<T> sendRequestPacket(Function<Long, RequestPacket> packetConstruction, String destination, Function<Object, T> resultParser, int timeoutSeconds) {
        if(id == null || !isConnected()) return disconnectedRequest();
        Request<T> request = request(resultParser, timeoutSeconds);
        RequestPacket packet = packetConstruction.apply(request.getRequestId());

        String packetString = gson.toJson(packet);
        System.out.println("Send to " + id + ": " + packetString);

        ConnectionTransfer outputPacket = new ConnectionTransfer(packet.getPacketId(), packetString, id,
                destination, System.currentTimeMillis());
        out.println(gson.toJson(outputPacket));
        return request;
    }

    /**
     * Sends the retrieved response into the requests' future.
     * @param requestId The ID of the request.
     * @param response The retrieved response.
     * @return True whether a request with this ID exists.
     * Or {@code true} if this invocation caused the CompletableFuture
     * to transition to a completed state, else {@code false}.
     * @param <T> the response packet type
     */
    @Override
    public <T extends ResponsePacket<Object>> boolean retrieveResponse(long requestId, T response) {
        return response(requestId, response);
    }

    /**
     * Returns whether this connection is connected.
     * This is only the case when the connection is bound.
     * @return True if is it connected.
     */
    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && socket.isBound() && !socket.isClosed();
    }

    /**
     * Closes the connection by sending a close packet and closing the connection.
     * This never tries to reconnect after closing, use either {@link #closeAndReconnect()} or run {@link #reconnect()} afterward.
     * The close packet is only sent when the connection is properly started before.
     */
    @Override
    public void close() {
        if(isConnected()) sendPacket(new ConnectionClosePacket(), null);
        if(scheduledKeepAlive != null) scheduledKeepAlive.cancel(true);
        scheduledKeepAlive = null;
        stopTask();
        if(!reconnect) stopExecutor();
        if(readThread != null && readThread.isAlive()) readThread.interrupt();
        readThread = null;
        try {
            if(in != null) in.close();
            if(out != null) out.close();
            if(socket != null) socket.close();
        } catch (IOException ignored) {
        } finally {
            in = null;
            out = null;
            socket = null;
            started = false;
            if(reconnect && onClose != null) onClose.run();
            if(!reconnect && onFinalClose != null) onFinalClose.run();
        }
    }

    /**
     * Reconnects the connection.
     * This will only reconnect {@link #reconnect} when is true, the reconnect will only start after the {@link #secondsBetweenReconnections}.
     */
    @Override
    public void reconnect() {
        if(started || !reconnect) return;
        Runnable reconnectRunnable = new Runnable() {
            @Override
            public void run() {
                if(started) {
                    if(reconnect) getScheduledExecutor().schedule(this, secondsBetweenReconnections, TimeUnit.SECONDS);
                    return;
                }
                try {
                    connect();
                    if(onReconnect != null) onReconnect.run();
                } catch (IOException e) {
                    // We could not properly connect, fully restore.
                    close();
                    if(reconnect) getScheduledExecutor().schedule(this, secondsBetweenReconnections, TimeUnit.SECONDS);
                }
            }
        };
        getScheduledExecutor().schedule(reconnectRunnable, secondsBetweenReconnections, TimeUnit.SECONDS);
    }

    /**
     * Closes the connection upon which it is being reconnected.
     * This is useful for when the connection has gone down and a reconnect is preferred.
     * This will only reconnect {@link #reconnect} when is true, the reconnect will only start after the {@link #secondsBetweenReconnections}.
     */
    @Override
    public void closeAndReconnect() {
        close();
        if(reconnect) reconnect();
    }

    @Override
    public void setOnReconnect(Runnable onReconnect) {
        this.onReconnect = onReconnect;
    }

    /**
     * Disables reconnecting, this fully shutdown the executor tied to this connection.
     */
    @Override
    public void disableReconnect() {
        reconnect = false;
        if(!started) stopExecutor();
    }

    public boolean isReconnect() {
        return reconnect;
    }

    private void checkKeepAlive() {
        sendRequestPacket(id -> new ConnectionKeepAlivePacket(id, System.currentTimeMillis()), null).getFutureResult().whenComplete((result, exception) -> {
            if(exception != null) {
                numberKeepAliveFails++;
                if(numberKeepAliveFails > maximumKeepAliveFails) {
                    closeAndReconnect();
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
        if(scheduledKeepAlive != null && started) {
            scheduledKeepAlive.cancel(true);
            scheduledKeepAlive = getScheduledExecutor().scheduleAtFixedRate(this::checkKeepAlive, secondsBetweenKeepAliveChecks, secondsBetweenKeepAliveChecks, TimeUnit.SECONDS);
        }
    }

    public void setSecondsBetweenReconnections(int secondsBetweenReconnections) {
        if(secondsBetweenReconnections <= 0) return;
        this.secondsBetweenReconnections = secondsBetweenReconnections;
    }
}
