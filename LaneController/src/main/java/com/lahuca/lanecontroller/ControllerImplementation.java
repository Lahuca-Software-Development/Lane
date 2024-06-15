package com.lahuca.lanecontroller;

import com.lahuca.lane.connection.request.Result;
import com.lahuca.lane.message.LaneMessage;

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


}