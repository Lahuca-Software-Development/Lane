package com.lahuca.lanecontroller;

import com.lahuca.lane.LaneGameState;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class ControllerGame {

    private final long gameId;
    private String serverId;
    private String name;
    private LaneGameState state;

    public ControllerGame(long gameId, String serverId, String name, LaneGameState state) {
        this.gameId = gameId;
        this.serverId = serverId;
        this.name = name;
        this.state = state;
    }

    public long getGameId() {
        return gameId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
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

    public void setState(LaneGameState state) {
        this.state = state;
    }
}
