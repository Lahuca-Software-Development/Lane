/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 21-3-2024 at 12:20 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.laneinstance;

import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.records.PlayerStateRecord;
import com.lahuca.lane.records.StatePropertyRecord;

import java.util.*;

public class InstancePlayerState implements LanePlayerState {

    private String name;
    private final HashMap<String, StateProperty> properties;

    public InstancePlayerState() {
        this(UNDEFINED);
    }

    public InstancePlayerState(String name) {
        this.name = name;
        properties = new HashMap<>();
    }

    public InstancePlayerState(String name, HashMap<String, StateProperty> properties) {
        this.name = name;
        this.properties = properties;
    }

    public InstancePlayerState(String name, Collection<StateProperty> properties) {
        this(name);
        properties.forEach(property -> this.properties.put(property.getId(), property));
    }

    public InstancePlayerState(PlayerStateRecord record) {
        this();
        applyRecord(record);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HashMap<String, StateProperty> getProperties() {
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
        Set<String> keys = new HashSet<>(properties.keySet());
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

    @Override
    public String toString() {
        return new StringJoiner(", ", InstancePlayerState.class.getSimpleName() + "[", "]").add("name='" + name + "'").add("properties=" + properties).toString();
    }
}
