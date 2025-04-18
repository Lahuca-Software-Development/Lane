package com.lahuca.lane;

import com.lahuca.lane.connection.Connection;

import java.io.IOException;

/**
 * Extends the base class {@link Connection} by also providing methods to reconnect the connection when it is closed.
 * The default behaviour of {@link Connection#close()} is to not reconnect.
 */
public interface ReconnectConnection extends Connection {

    /**
     * Tries to connect.
     */
    void connect() throws IOException;

    /**
     * Tries to reconnect the connection.
     * This typically resets to the original state, after which {@link #connect()} is called.
     */
    void reconnect();

    /**
     * Closes the connection upon which it is being reconnected.
     * This is useful for when the connection has gone down and a reconnect is preferred.
     * This will only reconnect when it is allowed within the tied connection.
     */
    void closeAndReconnect();

    /**
     * Sets the runnable to run when a reconnect has happened.
     * @param onReconnect the runnable to run.
     */
    void setOnReconnect(Runnable onReconnect);

    /**
     * Disables reconnecting.
     */
    void disableReconnect();

}
