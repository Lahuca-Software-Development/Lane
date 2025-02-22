package com.lahuca.laneinstance;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lane.records.PlayerRecord;

import java.util.*;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class InstancePlayer implements LanePlayer {


    private final UUID uuid;
    private final String name;
    private String displayName;
    private Locale language;
    private QueueRequest queueRequest;
    private String instanceId = null;
    private Long gameId = null;
    private InstancePlayerState state = null;
    private Long partyId = null;
    private Set<Long> relationships;

    public InstancePlayer(UUID uuid, String name, String displayName) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
    }

    public InstancePlayer(PlayerRecord record) {
        this.uuid = record.uuid();
        this.name = record.name();
        applyRecord(record);
    }

    public void setLanguage(String languageTag) {
        setLanguage(Locale.forLanguageTag(languageTag));
    }

    public void setLanguage(Locale language) {
        this.language = language;
        //LaneInstance.getInstance().sendController(new InstanceUpdatePlayerPacket(convertRecord())); // TODO Use something different. Never use InstanceUpdatePlayerPacket for if it is going to the controller.
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
        return Optional.empty();
    }

    @Override
    public Optional<Long> getGameId() {
        return Optional.ofNullable(gameId);
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
        return state;
    }

    @Override
    public Optional<Long> getPartyId() {
        return Optional.ofNullable(partyId);
    }

    @Override
    public Set<Long> getRelationships() {
        return relationships;
    }

    @Override
    public PlayerRecord convertRecord() {
        return new PlayerRecord(uuid, name, displayName, language.toLanguageTag(), queueRequest, instanceId, gameId, state.convertRecord(), partyId);
    }

    @Override
    public void applyRecord(PlayerRecord record) {
        // TODO Maybe better recode?
        displayName = record.displayName();
        language = Locale.forLanguageTag(record.languageTag());
        queueRequest = record.queueRequest();
        instanceId = record.instanceId();
        gameId = record.gameId();
        if(state == null) state = new InstancePlayerState();
        state.applyRecord(record.state());
        partyId = record.partyId();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InstancePlayer.class.getSimpleName() + "[", "]").add("uuid=" + uuid).add("name='" + name + "'").add("displayName='" + displayName + "'").add("language=" + language).add("queueRequest=" + queueRequest).add("instanceId='" + instanceId + "'").add("gameId=" + gameId).add("state=" + state).add("partyId=" + partyId).add("relationships=" + relationships).toString();
    }

}
