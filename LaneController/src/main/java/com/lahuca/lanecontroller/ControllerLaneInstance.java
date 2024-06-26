/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 23-3-2024 at 12:16 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller;

public class ControllerLaneInstance {

    private String id;
    private boolean joinable;
    private boolean nonPlayable; // Tells whether the instance is also non playable: e.g. lobby
    private int currentPlayers;
    private int maxPlayers; // Maximum number of players on the instance, negative = unlimited
    // TODO Add: boolean isMaintanance (only for on the controller)

    public ControllerLaneInstance(String id) {
        this.id = id;
        joinable = true;
        nonPlayable = false;
    }

    public ControllerLaneInstance(String id, boolean joinable, boolean nonPlayable, int currentPlayers, int maxPlayers) {
        this.id = id;
        this.joinable = joinable;
        this.nonPlayable = nonPlayable;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
    }

    public String getId() {
        return id;
    }

    public boolean isJoinable() {
        return joinable;
    }

    public boolean isNonPlayable() {
        return nonPlayable;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void update(boolean joinable, boolean nonPlayable, int currentPlayers, int maxPlayers) {
        this.joinable = joinable;
        this.nonPlayable = nonPlayable;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
    }

}
