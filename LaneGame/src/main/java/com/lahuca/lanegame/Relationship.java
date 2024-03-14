package com.lahuca.lanegame;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LaneRelationship;

import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class Relationship implements LaneRelationship {

    private LanePlayer one;
    private LanePlayer two;

    private UUID request;

    public Relationship(LanePlayer one, LanePlayer two, UUID request) {
        this.one = one;
        this.two = two;
        this.request = request;
    }

    @Override
    public LanePlayer getOne() {
        return one;
    }

    @Override
    public LanePlayer getTwo() {
        return two;
    }

    @Override
    public UUID getRequest() {
        return request;
    }

    public void setOne(LanePlayer one) {
        this.one = one;
    }

    public void setTwo(LanePlayer two) {
        this.two = two;
    }

    public void setRequest(UUID request) {
        this.request = request;
    }
}
