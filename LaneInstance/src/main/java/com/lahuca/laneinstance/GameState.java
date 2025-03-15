package com.lahuca.laneinstance;

import com.lahuca.lane.LaneGameState;
import com.lahuca.lane.LaneStateProperty;
import com.lahuca.lane.records.GameStateRecord;
import com.lahuca.lane.records.StatePropertyRecord;

import java.util.HashMap;
import java.util.Set;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/

public class GameState implements LaneGameState {

    private String name;
    private boolean joinable;
    private boolean playable;
    private final HashMap<String, StateProperty> properties = new HashMap<>();

    public GameState(String name, boolean joinable, boolean playable) {
        this.name = name;
        this.joinable = joinable;
        this.playable = playable;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isJoinable() {
        return joinable;
    }

    @Override
    public boolean isPlayable() {
        return playable;
    }

    @Override
    public HashMap<String, ? extends LaneStateProperty> getProperties() {
        return properties;
    }

    @Override
    public GameStateRecord convertRecord() {
        HashMap<String, StatePropertyRecord> propertyRecords = new HashMap<>();
        properties.forEach((k, v) -> propertyRecords.put(k, v.convertRecord()));
        return new GameStateRecord(name, joinable, playable, propertyRecords);
    }

    @Override
    public void applyRecord(GameStateRecord record) {
        this.name = record.name();
        this.joinable = record.isJoinable();
        this.playable = record.isPlayable();
        Set<String> keys = properties.keySet();
        record.properties().forEach((k, v) -> {
            if(keys.contains(k)) {
                properties.get(k).applyRecord(v);
                keys.remove(k);
            } else {
                properties.put(k, new StateProperty(v.id(), v.value(), v.extraData()));
            }
        });
        keys.forEach(properties::remove);
    }

}
