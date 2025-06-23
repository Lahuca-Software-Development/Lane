package com.lahuca.laneinstance;

import com.lahuca.lane.LaneStateProperty;
import com.lahuca.lane.records.StatePropertyRecord;

import java.util.StringJoiner;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class InstanceStateProperty implements LaneStateProperty {

    private String id;
    private Object value;
    private Object extraData;

    public InstanceStateProperty(String id, Object value) {
        this(id, value, null);
    }

    public InstanceStateProperty(String id, Object value, Object extraData) {
        this.id = id;
        this.value = value;
        this.extraData = extraData;
    }

    public InstanceStateProperty(StatePropertyRecord record) {
        applyRecord(record);
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

    @Override
    public StatePropertyRecord convertRecord() {
        return new StatePropertyRecord(id, value, extraData);
    }

    @Override
    public void applyRecord(StatePropertyRecord record) {
        id = record.id();
        value = record.value();
        extraData = record.extraData();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InstanceStateProperty.class.getSimpleName() + "[", "]").add("id='" + id + "'").add("value=" + value).add("extraData=" + extraData).toString();
    }

}
