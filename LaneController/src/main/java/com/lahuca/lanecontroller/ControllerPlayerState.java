package com.lahuca.lanecontroller;

import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.LaneStateProperty;

import java.util.HashMap;

/**
 * @author _Neko1
 * @date 16.03.2024
 **/
public class ControllerPlayerState implements LanePlayerState {

    private final String name;
    private final HashMap<String, ControllerStateProperty> properties;

    public ControllerPlayerState(String name, HashMap<String, ControllerStateProperty> properties) {
        this.name = name;
        this.properties = properties;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HashMap<String, ? extends LaneStateProperty> getProperties() {
        return properties;
    }
}
