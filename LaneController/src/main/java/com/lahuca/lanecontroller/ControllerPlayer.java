/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 17:30 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LanePlayerState;

import java.util.Optional;
import java.util.UUID;

public class ControllerPlayer implements LanePlayer {

    private ControllerRelationship relationship;

    private final UUID uuid;
    private final String name;
    private String displayName;
    private String language;

    private ControllerPlayerState playerState;
    private ControllerParty controllerParty;


    public ControllerPlayer(ControllerRelationship relationship, UUID uuid, String name, String displayName, String language, ControllerPlayerState playerState, ControllerParty controllerParty) {
        this.relationship = relationship;
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
        this.language = language;
        this.playerState = playerState;
        this.controllerParty = controllerParty;
    }

    /**
     * Retrieves the set of relationships associated with this controller.
     *
     * @return The set of ControllerRelationship objects.
     */
    public Optional<ControllerRelationship> getRelationship() {
        return Optional.ofNullable(relationship);
    }

    /**
     * Sets the display name for this controller.
     *
     * @param displayName The new display name to be set.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Sets the requested state for this controller.
     *
     * @param playerState The ControllerPlayerState to be set.
     */
    public void setPlayerState(ControllerPlayerState playerState) {
        this.playerState = playerState;
    }

    /**
     * Sets the party associated with this controller.
     *
     * @param controllerParty The ControllerParty to be set.
     */
    public void setParty(ControllerParty controllerParty) {
        this.controllerParty = controllerParty;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public LanePlayerState getState() {
        return playerState;
    }

    @Override
    public Optional<ControllerParty> getParty() {
        return Optional.ofNullable(controllerParty);
    }
}
