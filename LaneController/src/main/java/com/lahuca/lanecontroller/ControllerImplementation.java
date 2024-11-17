package com.lahuca.lanecontroller;

import com.lahuca.lane.connection.request.Result;
import com.lahuca.lane.message.LaneMessage;
import com.lahuca.lanecontroller.events.QueueStageEvent;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/
public interface ControllerImplementation {

    /**
     * Method that will switch the server of the given player to the given server.
     * This should not do anything when the player is already connected to the given server.
     * @param uuid the player's uuid
     * @param destination the server's id
     * @return the completable future with the result
     */
    CompletableFuture<Result<Void>> joinServer(UUID uuid, String destination);

    /**
     * Gets the translator to be used for messages on the proxy.
     * @return the translator
     */
    LaneMessage getTranslator();

    /**
     * Gets a new ControllerLaneInstance for the given ControllerPlayer to join.
     * This instance is not intended to be played at a game at currently.
     * @param controller the controller requesting the new instance
     * @param player the player requesting a new instance
     * @param exclude the collection of instances to exclude from the output
     * @return the instance to go to, if the optional is null, then no instance could be found
     */
    Optional<ControllerLaneInstance> getNewInstance(Controller controller, ControllerPlayer player, Collection<ControllerLaneInstance> exclude);

    /**
     * Lets the implemented controller handle the {@link QueueStageEvent}.
     * Do not do blocking actions while handling the event, as often a "direct" response is needed.
     * @param controller The controller that is handling this queue event
     * @param event The event to handle.
     */
    void handleQueueStageEvent(Controller controller, QueueStageEvent event);

    /**
     * Send a message to the player with the given UUID.
     * @param player The player's UUID
     * @param message The message to send
     * @return true whether the message was successfully sent
     */
    boolean sendMessage(UUID player, String message);

    /**
     * Disconnect the player with the given message from the network.
     * @param player The player's UUID
     * @param message The message to show when disconnecting
     * @return true whether the player was successfully disconnected.
     */
    boolean disconnectPlayer(UUID player, String message);


}