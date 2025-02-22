package com.lahuca.lanecontroller;

import com.lahuca.lane.LaneGameState;
import com.lahuca.lane.records.GameStateRecord;
import com.lahuca.lane.records.StatePropertyRecord;

import java.util.HashMap;
import java.util.Set;
import java.util.StringJoiner;

/**
 * @author _Neko1
 * @date 16.03.2024
 **/
public class ControllerGameState implements LaneGameState {

    private String name;
    private boolean isJoinable;
    private boolean isPlayable;
    private final HashMap<String, ControllerStateProperty> properties = new HashMap<>();

    public ControllerGameState(GameStateRecord record) {
        applyRecord(record);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isJoinable() {
        return isJoinable;
    }

    @Override
    public boolean isPlayable() {
        return isPlayable;
    }

    @Override
    public HashMap<String, ControllerStateProperty> getProperties() {
        return properties;
    }

    @Override
    public GameStateRecord convertRecord() {
        HashMap<String, StatePropertyRecord> propertyRecords = new HashMap<>();
        properties.forEach((k, v) -> propertyRecords.put(k, v.convertRecord()));
        return new GameStateRecord(name, isJoinable, isPlayable, propertyRecords);
    }

    @Override
    public void applyRecord(GameStateRecord record) {
        name = record.name();
        isJoinable = record.isJoinable();
        isPlayable = record.isPlayable();
        Set<String> keys = properties.keySet();
        record.properties().forEach((k, v) -> {
            if(keys.contains(k)) {
                properties.get(k).applyRecord(v);
                keys.remove(k);
            } else {
                properties.put(k, new ControllerStateProperty(v.id(), v.value(), v.extraData()));
            }
        });
        keys.forEach(properties::remove);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ControllerGameState.class.getSimpleName() + "[", "]").add("name='" + name + "'").add("isJoinable=" + isJoinable).add("isPlayable=" + isPlayable).add("properties=" + properties).toString();
    }
}
