package com.lahuca.lanecontroller;

import com.lahuca.lane.LaneStateProperty;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class ControllerStateProperty implements LaneStateProperty {

    private String id;
    private Object value;
    private Object extraData;

    public ControllerStateProperty(String id, Object value, Object extraData) {
        this.id = id;
        this.value = value;
        this.extraData = extraData;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Object getExtraData() {
        return extraData;
    }
}
