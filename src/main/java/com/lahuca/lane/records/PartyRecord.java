package com.lahuca.lane.records;

import com.lahuca.lane.LanePlayer;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/

public record PartyRecord(UUID owner, Set<LanePlayer> players, long creationStamp) {
}