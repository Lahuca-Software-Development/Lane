package com.lahuca.lanecontroller;

import com.lahuca.lane.LaneGameState;
import com.lahuca.lane.records.GameStateRecord;

import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class ControllerGame {

    private final UUID gameId;
    private String serverId;
    private String name;
    private final ControllerGameState state;

    public ControllerGame(UUID gameId, String serverId, String name, ControllerGameState state) {
        this.gameId = gameId;
        this.serverId = serverId;
        this.name = name;
        this.state = state;
    }

    public UUID getGameId() {
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

    public void setState(GameStateRecord state) {
        this.state.applyRecord(state);
    }

    public void update(String serverId, String name, GameStateRecord state) {
        setServerId(serverId);
        setName(name);
        setState(state);
    }

}
