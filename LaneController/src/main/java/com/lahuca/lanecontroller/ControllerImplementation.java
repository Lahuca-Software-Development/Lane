package com.lahuca.lanecontroller;

import java.util.UUID;

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
     */
    void joinServer(UUID uuid, String destination);

}