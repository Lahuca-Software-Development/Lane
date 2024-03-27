package com.lahuca.lanecontroller;

import com.lahuca.lane.connection.request.Result;

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

}