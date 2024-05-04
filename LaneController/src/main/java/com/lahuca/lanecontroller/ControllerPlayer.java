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
import com.lahuca.lane.records.PlayerRecord;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ControllerPlayer implements LanePlayer {

    private final UUID uuid;
    private final String name;
    private String displayName;
    private String language;
    private String instanceId = null;
    private Long gameId = null;
    private ControllerPlayerState state = null;
    private Long partyId = null;
    private final Set<Long> relationships = new HashSet<>();

    public ControllerPlayer(UUID uuid, String name, String displayName, String language) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
        this.language = language;
    }

    @Override
    public Set<Long> getRelationships() {
        return relationships;
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
    public String getLanguage() {
        return language;
    }

    @Override
    public Optional<String> getInstanceId() {
        return Optional.ofNullable(instanceId);
    }

    @Override
    public Optional<Long> getGameId() {
        return Optional.ofNullable(gameId);
    }

    @Override
    public LanePlayerState getState() {
        return state;
    }

    @Override
    public Optional<Long> getPartyId() {
        return Optional.ofNullable(partyId);
    }

    /**
     * Sets the display name for this controller.
     *
     * @param displayName The new display name to be set.
     */
    public void setDisplayName(String displayName) {
        // TODO Simple setter? Maybe packet send needed?
        this.displayName = displayName;
    }

    /**
     * Sets the state of the player.
     *
     * @param state the state
     */
    public void setState(ControllerPlayerState state) {
        // TODO Simple setter? Maybe packet send needed?
        this.state = state;
    }

    public void addRelationship(Long id) {
        relationships.add(id);
    } // TODO

    public void removeRelationship(Long id) {
        relationships.remove(id);
    } // TODO

    /**
     * Sets the party of the player.
     * This method is only to be called from the controller, as it sets the variable without any side-effects.
     * @param partyId the party ID to set
     */
    void setPartyByController(Long partyId) {
        this.partyId = partyId;
    }

    /**
     * Sets the party associated with this player.
     * This method is only to be used with the controller, who manages the following:
     * Also remove player from the party object, send correct player updates.
     * @param controller the controller
     * @param partyId the partyId to be set.
     */
    public void setParty(long partyId) {
        // TODO Simple setter? Maybe packet send needed?
        this.partyId = partyId;
    }

    public void setGameId(long gameId) {
        // TODO Simple setter? Maybe packet send needed?
        this.gameId = gameId;
    }

    @Override
    public PlayerRecord convertRecord() {
        return new PlayerRecord(uuid, name, displayName, language, instanceId, gameId, state.convertRecord(), partyId);
    }

    @Override
    public void applyRecord(PlayerRecord record) {
        // TODO Recode this. When is this even called on the ControllerPlayer object? Never?
        displayName = record.displayName();
        language = record.language();
        instanceId = record.instanceId();
        gameId = record.gameId();
        if(state == null) state = new ControllerPlayerState();
        state.applyRecord(record.state());
        partyId = record.partyId();
    }

    public void load() {
        //TODO load relationship data
    }

    public void save() {
        //TODO save relationship data

    }
}
