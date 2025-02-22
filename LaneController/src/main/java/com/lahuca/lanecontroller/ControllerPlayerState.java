package com.lahuca.lanecontroller;

import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.records.PlayerStateRecord;
import com.lahuca.lane.records.StatePropertyRecord;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.StringJoiner;

/**
 * @author _Neko1
 * @date 16.03.2024
 **/
public class ControllerPlayerState implements LanePlayerState {

    private String name;
    private final HashMap<String, ControllerStateProperty> properties;

    public ControllerPlayerState() {
        this(UNDEFINED);
    }

    public ControllerPlayerState(String name) {
        this.name = name;
        properties = new HashMap<>();
    }

    public ControllerPlayerState(String name, HashMap<String, ControllerStateProperty> properties) {
        this.name = name;
        this.properties = properties;
    }

    public ControllerPlayerState(String name, Collection<ControllerStateProperty> properties) {
        this(name);
        properties.forEach(property -> this.properties.put(property.getId(), property));
    }

    public ControllerPlayerState(PlayerStateRecord record) {
        this();
        applyRecord(record);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HashMap<String, ControllerStateProperty> getProperties() {
        return properties;
    }

    @Override
    public PlayerStateRecord convertRecord() {
        HashMap<String, StatePropertyRecord> propertyRecords = new HashMap<>();
        properties.forEach((k, v) -> propertyRecords.put(k, v.convertRecord()));
        return new PlayerStateRecord(name, propertyRecords);
    }

    @Override
    public void applyRecord(PlayerStateRecord record) {
        name = record.name();
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
        return new StringJoiner(", ", ControllerPlayerState.class.getSimpleName() + "[", "]").add("name='" + name + "'").add("properties=" + properties).toString();
    }
}
