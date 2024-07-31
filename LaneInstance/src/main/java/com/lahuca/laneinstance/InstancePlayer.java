package com.lahuca.laneinstance;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.connection.packet.InstanceUpdatePlayerPacket;
import com.lahuca.lane.records.PlayerRecord;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class InstancePlayer implements LanePlayer {


    private final UUID uuid;
    private final String name;
    private String displayName;
    private Locale language;
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

    public void setLanguage(Locale language) {
        this.language = language;
        LaneInstance.getInstance().sendController(new InstanceUpdatePlayerPacket(convertRecord()));
    }

    @Override
    public Locale getLanguage() {
        return language;
    }

    @Override
    public Optional<String> getInstanceId() {
        return Optional.empty();
    }

    public void setGameId(long gameId) {
        this.gameId = gameId;
        LaneInstance.getInstance().sendController(new InstanceUpdatePlayerPacket(convertRecord()));
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
        return new PlayerRecord(uuid, name, displayName, language, instanceId, gameId, state.convertRecord(), partyId);
    }

    @Override
    public void applyRecord(PlayerRecord record) {
        // TODO Maybe better recode?
        displayName = record.displayName();
        language = record.languageTag();
        instanceId = record.instanceId();
        gameId = record.gameId();
        if(state == null) state = new InstancePlayerState();
        state.applyRecord(record.state());
        partyId = record.partyId();
    }

}
