package com.lahuca.lanecontroller;

import com.lahuca.lane.LaneGameState;
import com.lahuca.lane.records.GameStateRecord;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class ControllerGame {

    private final long gameId;
    private final String instanceId;
    private String name;
    private final ControllerGameState state;

    public ControllerGame(long gameId, String instanceId, String name, ControllerGameState state) {
        this.gameId = gameId;
        this.instanceId = instanceId;
        this.name = name;
        this.state = state;
    }

    public long getGameId() {
        return gameId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LaneGameState getState() {
        return state;
    }

    public void setState(GameStateRecord state) {
        this.state.applyRecord(state);
    }

    public void update(String name, GameStateRecord state) {
        setName(name);
        setState(state);
    }

}
