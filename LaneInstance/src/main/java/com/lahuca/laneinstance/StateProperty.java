package com.lahuca.laneinstance;

import com.lahuca.lane.LaneStateProperty;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class StateProperty implements LaneStateProperty {

    private String id;
    private Object value;
    private Object extraData;

    public StateProperty(String id, Object value, Object extraData) {
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
