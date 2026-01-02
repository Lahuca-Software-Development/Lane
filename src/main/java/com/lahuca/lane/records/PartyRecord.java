package com.lahuca.lane.records;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/

public record PartyRecord(long partyId, UUID owner, HashSet<UUID> players, boolean invitationsOnly,
                          long creationTimestamp, Set<UUID> unmodifiableInvitations) {

}