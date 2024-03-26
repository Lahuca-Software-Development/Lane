package com.lahuca.lanecontroller;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.records.PartyRecord;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class ControllerParty implements LaneParty {

    private final long partyId;
    private UUID owner;

    private final Set<UUID> players;
    private final Set<UUID> invited;
    private long creationStamp;

    public ControllerParty(long partyId, UUID owner) {
        this.partyId = partyId;
        this.owner = owner;
        this.players = new HashSet<>();
        this.invited = new HashSet<>();
        this.creationStamp = System.currentTimeMillis();
    }

    public void addPlayer(ControllerPlayer controllerPlayer) {
        players.add(controllerPlayer.getUuid());
    }

    public void addPlayer(UUID uuid) {
        players.add(uuid);
    }

    public void sendRequest(ControllerPlayer controllerPlayer) {
        invited.add(controllerPlayer.getUuid());
    }

    public void removeRequest(ControllerPlayer controllerPlayer) {
        invited.remove(controllerPlayer.getUuid());
    }

    public void removePlayer(ControllerPlayer controllerPlayer) {
        players.remove(controllerPlayer.getUuid());
        controllerPlayer.setParty(-1);
    }

    public void disband() {
        players.clear();
        invited.clear();
        creationStamp = -1;
    }

    public boolean contains(UUID uuid) {
        return getPlayers().stream().anyMatch(player -> player.equals(uuid));
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public long getCreationStamp() {
        return creationStamp;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public long getId() {
        return partyId;
    }

    @Override
    public Set<UUID> getPlayers() {
        return players;
    }

    public Set<UUID> getInvited() {
        return invited;
    }

    @Override
    public long getCreationTimestamp() {
        return creationStamp;
    }

    public PartyRecord convertToRecord() {
        return new PartyRecord(owner, players, creationStamp);
    }
}
