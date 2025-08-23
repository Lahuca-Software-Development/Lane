/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 19-3-2024 at 13:27 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.records;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public record GameRecord(long gameId, String instanceId, String gameType, String gameMode, String gameMap,
                         HashSet<UUID> reserved, HashSet<UUID> online, HashSet<UUID> players,
                         HashSet<UUID> playing, boolean onlineJoinable, boolean playersJoinable,
                         boolean playingJoinable, int maxOnlineSlots, int maxPlayersSlots, int maxPlayingSlots,
                         boolean onlineKickable, boolean playersKickable, boolean playingKickable,
                         String state, HashMap<String, StatePropertyRecord> properties) {
}
