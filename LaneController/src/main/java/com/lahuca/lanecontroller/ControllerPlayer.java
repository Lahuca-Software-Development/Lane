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

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LanePlayerState;

import java.util.*;

public class ControllerPlayer implements LanePlayer {

    private final Set<ControllerRelationship> relationships;

    private final UUID uuid;
    private final String name;
    private String displayName;

    private ControllerPlayerState playerState;
    private ControllerParty controllerParty;

    public ControllerPlayer(UUID uuid, String name, String displayName, ControllerParty controllerParty, ControllerPlayerState playerState) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
        this.controllerParty = controllerParty;
        this.playerState = playerState;

        this.relationships = new HashSet<>();
    }

    /**
     * Adds a relationship to the list of relationships associated with this controller.
     *
     * @param controllerPlayer The ControllerPlayer to be added.
     */
    public void addRelationship(ControllerPlayer controllerPlayer) {
        relationships.add(new ControllerRelationship(this, controllerPlayer, uuid));
    }

    /**
     * Removes a relationship from the list of relationships associated with this controller.
     *
     * @param controllerPlayer The ControllerPlayer to be removed.
     */
    public void removeRelationship(ControllerPlayer controllerPlayer) {
        getRelationship(controllerPlayer).ifPresent(relationships::remove);
    }

    public Optional<ControllerRelationship> getRelationship(ControllerPlayer controllerPlayer) {
        return relationships.stream().filter(relationship -> relationship.getTwo() == controllerPlayer).findFirst();
    }

    /**
     * Retrieves the set of relationships associated with this controller.
     *
     * @return The set of ControllerRelationship objects.
     */
    public Set<ControllerRelationship> getRelationships() {
        return relationships;
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
     * Sets the player state for this controller.
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
    public Optional<LaneParty> getParty() {
        return Optional.ofNullable(controllerParty);
    }

}
