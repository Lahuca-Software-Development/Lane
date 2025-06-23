package com.lahuca.lane.records;

import java.util.HashSet;
import java.util.UUID;

public record InstanceRecord(String id, String type, HashSet<UUID> reserved, HashSet<UUID> online, HashSet<UUID> players,
                             HashSet<UUID> playing, boolean onlineJoinable, boolean playersJoinable,
                             boolean playingJoinable, int maxOnlineSlots, int maxPlayersSlots, int maxPlayingSlots) {
}
