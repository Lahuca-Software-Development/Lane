package com.lahuca.lane.records;

import java.util.Set;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 19.03.2024
 **/

public record RelationshipRecord(Long relationshipId, Set<UUID> players) {
}
