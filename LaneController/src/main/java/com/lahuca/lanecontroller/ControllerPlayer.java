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
import com.lahuca.lane.connection.packet.InstanceUpdatePlayerPacket;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lane.records.PlayerRecord;

import java.util.*;

public class ControllerPlayer implements LanePlayer {

    private final UUID uuid;
    private final String username;
    private String displayName;
    private Locale language;
    private QueueRequest queueRequest;
    private String instanceId = null;
    private Long gameId = null;
    private ControllerPlayerState state = null;
    private Long partyId = null;

    public ControllerPlayer(UUID uuid, String username, String displayName, Locale language) {
        this.uuid = uuid;
        this.username = username;
        this.displayName = displayName;
        this.language = language;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Locale getLanguage() {
        return language;
    }

    @Override
    public Optional<QueueRequest> getQueueRequest() {
        return Optional.ofNullable(queueRequest);
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
    public ControllerPlayerState getState() {
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
        this.displayName = displayName;
        updateInstancePlayer();
    }

    public void setQueueRequest(QueueRequest queueRequest) {
        this.queueRequest = queueRequest; // TODO This should be cleared when the request is succesfully done!
        // TODO The instance should reset this. Or we interact when we receive the updated player state.
        updateInstancePlayer();
    }

    /**
     * Sets the state of the player.
     *
     * @param state the state
     */
    public void setState(ControllerPlayerState state) {
        this.state = state;
        updateInstancePlayer();
    }

    /**
     * Sets the party associated with this controller.
     *
     * @param partyId The partyId to be set.
     */
    public void setParty(Long partyId) {
        this.partyId = partyId;
        updateInstancePlayer();
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        updateInstancePlayer();
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
        updateInstancePlayer();
    }

    @Override
    public PlayerRecord convertRecord() {
        return new PlayerRecord(uuid, username, displayName, language.toLanguageTag(), queueRequest, instanceId, gameId, state.convertRecord(), partyId);
    }

    /**
     * Update this player object with the given data.
     * This should never be called after initialization.
     * @param record the record with the data
     */
    @Override
    public void applyRecord(PlayerRecord record) {
        displayName = record.displayName();
        language = Locale.forLanguageTag(record.languageTag());
        queueRequest = record.queueRequest();
        instanceId = record.instanceId();
        gameId = record.gameId();
        if(state == null) state = new ControllerPlayerState();
        state.applyRecord(record.state());
        partyId = record.partyId();
    }

    public void updateInstancePlayer() {
        getInstanceId().ifPresent(instanceId -> Controller.getInstance().getConnection().sendPacket(new InstanceUpdatePlayerPacket(convertRecord()), instanceId));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ControllerPlayer.class.getSimpleName() + "[", "]").add("uuid=" + uuid).add("username='" + username + "'").add("displayName='" + displayName + "'").add("language=" + language).add("queueRequest=" + queueRequest).add("instanceId='" + instanceId + "'").add("gameId=" + gameId).add("state=" + state).add("partyId=" + partyId).toString();
    }
}
