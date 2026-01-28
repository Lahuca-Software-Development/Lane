/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 13-3-2024 at 21:31 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller;

import com.google.gson.Gson;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.socket.server.ServerSocketConnection;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.events.LaneEvent;
import com.lahuca.lanecontroller.events.InstanceUnregisterEvent;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * This is the main class for operations on the controller side of the Lane system.
 */
public abstract class Controller {

    // TODO For player objects, check wheher sendMessage works properly.
    //  We might want to add Audience to ControllerPlayer and InstancePlayer!
    // TODO Allow players to rejoin
    // TODO RDF for data objects?
    // TODO Add values to Party events!

    private static Controller instance;

    public static Controller getInstance() {
        return instance;
    }

    /**
     * Retrieves the player associated with the given UUID statically from the controller.
     * It is preferred to use the {@link ControllerPlayerManager#getPlayer(UUID)} method on an instance instead.
     * If the player is not found, an empty {@link Optional} is returned.
     *
     * @param uuid the unique identifier of the player
     * @return an {@link Optional} containing the {@link ControllerPlayer} if found,
     * or an empty {@link Optional} if the player does not exist
     */
    public static Optional<ControllerPlayer> getPlayer(UUID uuid) {
        return getInstance().getPlayerManager().getPlayer(uuid);
    }

    private final Gson gson;

    private final Connection connection;
    private final DataManager dataManager;

    private final ControllerDataManager controllerDataManager;
    private final ControllerPlayerManager playerManager;
    private final ControllerPartyManager partyManager;
    private final ControllerFriendshipManager friendshipManager;

    private final HashMap<Long, ControllerGame> games = new HashMap<>(); // Games are only registered because of instances
    private final HashMap<String, ControllerLaneInstance> instances = new HashMap<>(); // Additional data for the instances


    public Controller(Gson gson, Connection connection, DataManager dataManager) throws IOException {
        instance = this;
        this.gson = gson;
        this.connection = connection;
        this.dataManager = dataManager;
        controllerDataManager = new ControllerDataManager(this, dataManager, gson);
        playerManager = new ControllerPlayerManager(this, dataManager);
        partyManager = new ControllerPartyManager(this, dataManager);
        friendshipManager = new ControllerFriendshipManager(this, dataManager, gson);

        Packet.registerPackets();

        if (connection instanceof ServerSocketConnection serverSocketConnection) {
            // TODO Definitely change the type!
            serverSocketConnection.setOnClientRemove(id -> {
                ControllerLaneInstance old = instances.remove(id);
                if(old != null) handleControllerEvent(new InstanceUnregisterEvent(old));
                // Kick players.
                // TODO Maybe run some other stuff when it is done? Like kicking players. Remove the instance!
            });
        }
        connection.initialise(new ControllerInputPacket(this, dataManager, games, instances));
    }

    public void shutdown() {
        connection.close();
        dataManager.shutdown();
        // TODO Probably more
    }

    public Connection getConnection() {
        return connection;
    }

    private DataManager dataManager() {
        return dataManager;
    }

    public ControllerDataManager getDataManager() {
        return controllerDataManager;
    }

    public ControllerPlayerManager getPlayerManager() {
        return playerManager;
    }

    public ControllerPartyManager getPartyManager() {
        return partyManager;
    }

    public ControllerFriendshipManager getFriendshipManager() {
        return friendshipManager;
    }

    public Optional<ControllerLaneInstance> getInstance(String id) {
        return Optional.ofNullable(instances.get(id));
    } // TODO Really public?

    public Collection<ControllerLaneInstance> getInstances() { // TODO Really public?
        return instances.values();
    }


    public Collection<ControllerGame> getGames() {
        return games.values();
    } // TODO Redo

    public Optional<ControllerGame> getGame(long id) {
        return Optional.ofNullable(games.get(id));
    } // TODO Redo

    /**
     * Requests the instance to shut down the game.
     *
     * @param game the game to shut down
     * @return a {@link CompletableFuture} with a void to signify success: it has been shut down
     */
    public CompletableFuture<Void> shutdownGame(ControllerGame game) {
        return connection.<Void>sendRequestPacket(id -> new GameShutdownRequestPacket(id, game.getGameId()), game.getInstanceId()).getResult();
    }

    /**
     * Method that will switch the server of the given player to the given server.
     * This should not do anything when the player is already connected to the given server.
     *
     * @param uuid        the player's uuid
     * @param destination the server's id
     * @return the completable future with the result
     */
    public abstract CompletableFuture<Void> joinServer(UUID uuid, String destination);

    /**
     * Lets the implemented controller handle the Lane Controller event.
     * Some events have results tied to them, which are to be expected to return in the CompletableFuture.
     * Some events might not have the possibility to wait for the result asynchronously, so that the CompletableFuture is waited for.
     *
     * @param event the event to handle
     * @param <E>   the Lane controller event type
     * @return the CompletableFuture with the modified event
     */
    public abstract <E extends LaneEvent> CompletableFuture<E> handleControllerEvent(E event);


    // TODO These implementation dependent functions could technically also use ControllerPlayer instead of UUID?

    /**
     * Send a message to the player with the given UUID.
     *
     * @param player  the player's UUID
     * @param message the message to send
     */
    public abstract void sendMessage(UUID player, Component message);

    /**
     * Disconnect the player with the given message from the network.
     *
     * @param player  The player's UUID
     * @param message The message to show when disconnecting
     */
    public abstract void disconnectPlayer(UUID player, Component message);

    /**
     * Sets the effective locale for the given player.
     *
     * @param player the UUID of the player whose effective locale is being set
     * @param locale the locale to set as the effective locale for the player
     */
    public abstract void setEffectiveLocale(UUID player, Locale locale);

    /**
     * Gets the effective locale for the given player.
     *
     * @param player the UUID of the player whose effective locale is being retrieved
     * @return the effective locale, or null if the player is not online
     */
    public abstract Locale getEffectiveLocale(UUID player);

}