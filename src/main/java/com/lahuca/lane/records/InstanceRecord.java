package com.lahuca.lane.records;

public record InstanceRecord(String id, String type, boolean joinable, boolean nonPlayable, int currentPlayers, int maxPlayers) {
}
