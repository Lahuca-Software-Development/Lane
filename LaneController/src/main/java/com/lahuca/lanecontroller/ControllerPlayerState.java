package com.lahuca.lanecontroller;

import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.records.PlayerStateRecord;
import com.lahuca.lane.records.StatePropertyRecord;

import java.util.HashMap;
import java.util.Set;

/**
 * @author _Neko1
 * @date 16.03.2024
 **/
public class ControllerPlayerState implements LanePlayerState {

    private String name;
    private final HashMap<String, ControllerStateProperty> properties = new HashMap<>();

    public ControllerPlayerState(PlayerStateRecord record) {
        applyRecord(record); // TODO Maybe not really great, to only work with a record?
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

}
