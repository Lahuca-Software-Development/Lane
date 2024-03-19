package com.lahuca.lanecontroller;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.records.PartyRecord;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class ControllerParty implements LaneParty {

    private UUID owner;
    private final Set<LanePlayer> players;
    private final Set<UUID> requested;
    private long creationStamp;

    public ControllerParty(UUID owner, Set<LanePlayer> players, Set<UUID> requested, long creationStamp) {
        this.owner = owner;
        this.players = players;
        this.requested = requested;
        this.creationStamp = creationStamp;
    }

    public void addPlayer(ControllerPlayer controllerPlayer) {
        players.add(controllerPlayer);
    }

    public void sendRequest(ControllerPlayer controllerPlayer) {
        requested.add(controllerPlayer.getUuid());
    }

    public void removeRequest(ControllerPlayer controllerPlayer) {
        requested.remove(controllerPlayer.getUuid());
    }

    public void removePlayer(ControllerPlayer controllerPlayer) {
        players.remove(controllerPlayer);
    }

    public void disband() {
        players.clear();
        requested.clear();
        creationStamp = -1;
    }

    public boolean contains(UUID uuid) {
        return players().stream().anyMatch(lanePlayer -> lanePlayer.getUuid().equals(uuid));
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public Set<LanePlayer> players() {
        return players;
    }

    public Set<UUID> getRequested() {
        return requested;
    }

    @Override
    public long getCreationTimestamp() {
        return creationStamp;
    }

    public PartyRecord convertToRecord() {
        return new PartyRecord(owner, players, creationStamp);
    }
}
