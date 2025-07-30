package com.lahuca.lanecontroller;

import com.lahuca.lane.data.profile.ProfileData;
import com.lahuca.lane.data.profile.ProfileType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ControllerProfileData extends ProfileData {

    ControllerProfileData(UUID id, ProfileType type) {
        this(id, type, new HashSet<>(), new HashMap<>());
    }

    ControllerProfileData(UUID id, ProfileType type, HashSet<UUID> superProfiles, HashMap<String, HashMap<UUID, Boolean>> subProfiles) {
        super(id, type, superProfiles, subProfiles);
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
        if(name == null) {
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
        if(!(subProfile instanceof ControllerProfileData subProfileController)) {
            throw new IllegalArgumentException("subProfile is not a ControllerProfileData");
        }
        return Controller.getInstance().addSubProfile(this, subProfileController, name, active);
    }

    @Override
    public CompletableFuture<Boolean> removeSubProfile(ProfileData subProfile, String name) {
        if(!(subProfile instanceof ControllerProfileData subProfileController)) {
            throw new IllegalArgumentException("subProfile is not a ControllerProfileData");
        }
        return Controller.getInstance().removeSubProfile(this, subProfileController, name);
    }

    @Override
    public CompletableFuture<Void> resetProfile() {
        return Controller.getInstance().resetDeleteProfile(this, false);
    }

    @Override
    public CompletableFuture<Void> deleteProfile() {
        return Controller.getInstance().resetDeleteProfile(this, true);
    }

    @Override
    public CompletableFuture<Void> copyProfile(ProfileData from) {
        return Controller.getInstance().copyProfile(this, from);
    }

}
