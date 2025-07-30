package com.lahuca.laneinstance;

import com.lahuca.lane.data.profile.ProfileData;
import com.lahuca.lane.data.profile.ProfileType;
import com.lahuca.laneinstance.retrieval.Retrieval;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A profile data implementation for on the instance.
 * It is implementation-dependent if the information within this object is real time on the controller.
 */
public class InstanceProfileData extends ProfileData implements Retrieval {

    private final long retrievalTimestamp;

    InstanceProfileData(UUID id, ProfileType type) {
        this(id, type, new HashSet<>(), new HashMap<>());
    }

    InstanceProfileData(ProfileData profileData) {
        this(profileData.getId(), profileData.getType(), new HashSet<>(profileData.getSuperProfiles()), new HashMap<>(profileData.getSubProfiles()));
    }

    InstanceProfileData(UUID id, ProfileType type, HashSet<UUID> superProfiles, HashMap<String, HashMap<UUID, Boolean>> subProfiles) {
        super(id, type, superProfiles, subProfiles);
        retrievalTimestamp = System.currentTimeMillis();
    }

    // These methods are used internally to update the values
    void addSuperProfile(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        superProfiles.add(uuid);
    }

    void removeSuperProfile(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        superProfiles.remove(uuid);
    }

    void addSubProfile(UUID uuid, String name, boolean active) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        subProfiles.computeIfAbsent(name, k -> new HashMap<>()).put(uuid, active);
    }

    void removeSubProfile(UUID uuid, String name) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        if (name == null) {
            // Remove at all locations
            subProfiles.values().forEach(set -> set.remove(uuid));
        } else {
            // Remove at name
            if (!subProfiles.containsKey(name)) return;
            subProfiles.get(name).remove(uuid);
        }
        // Clear empty names
        subProfiles.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @Override
    public CompletableFuture<Boolean> addSubProfile(ProfileData subProfile, String name, boolean active) {
        if(!(subProfile instanceof InstanceProfileData subProfileInstance)) {
            throw new IllegalArgumentException("subProfile is not a InstanceProfileData");
        }
        return LaneInstance.getInstance().addSubProfile(this, subProfileInstance, name, active);
    }

    @Override
    public CompletableFuture<Boolean> removeSubProfile(ProfileData subProfile, String name) {
        if(!(subProfile instanceof InstanceProfileData subProfileInstance)) {
            throw new IllegalArgumentException("subProfile is not a InstanceProfileData");
        }
        return LaneInstance.getInstance().removeSubProfile(this, subProfileInstance, name);
    }

    @Override
    public CompletableFuture<Void> resetProfile() {
        return LaneInstance.getInstance().resetDeleteProfile(this, false);
    }

    @Override
    public CompletableFuture<Void> deleteProfile() {
        return LaneInstance.getInstance().resetDeleteProfile(this, true);
    }

    @Override
    public CompletableFuture<Void> copyProfile(ProfileData from) {
        return LaneInstance.getInstance().copyProfile(this, from);
    }

    @Override
    public long getRetrievalTimestamp() {
        return retrievalTimestamp;
    }

}
