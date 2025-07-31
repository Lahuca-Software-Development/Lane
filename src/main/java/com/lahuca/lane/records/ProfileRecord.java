package com.lahuca.lane.records;

import com.lahuca.lane.data.profile.ProfileType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public record ProfileRecord(UUID id, ProfileType type, HashSet<UUID> superProfiles, HashMap<String, HashMap<UUID, Boolean>> subProfiles) {
}
