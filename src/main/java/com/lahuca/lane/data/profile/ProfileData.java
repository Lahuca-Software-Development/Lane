package com.lahuca.lane.data.profile;

import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.data.RelationalId;
import com.lahuca.lane.data.manager.DataManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class ProfileData { // TODO Could this also be an interface?!? Look at LaneParty and Retrieval!

    private final UUID id;
    private final ProfileType type;
    protected HashSet<UUID> superProfiles;
    protected HashMap<String, HashSet<UUID>> subProfiles;

    public ProfileData(UUID id, ProfileType type, HashSet<UUID> superProfiles, HashMap<String, HashSet<UUID>> subProfiles) {
        this.id = id;
        this.type = type;
        this.superProfiles = superProfiles;
        this.subProfiles = subProfiles;
    }

    public UUID getId() {
        return id;
    }

    public DataObjectId getDataObjectId() {
        return new DataObjectId(RelationalId.Profiles(id), "data");
    }

    public ProfileType getType() {
        return type;
    }

    /**
     * Returns an unmodifiable set of the super profiles.
     *
     * @return the set
     */
    public Set<UUID> getSuperProfiles() {
        return Set.copyOf(superProfiles);
    }

    /**
     * Returns the first super profile.
     *
     * @return the super profile
     * @throws NoSuchElementException if there are no super profiles
     */
    public UUID getFirstSuperProfile() {
        if (superProfiles.isEmpty()) return null;
        return superProfiles.iterator().next();
    }

    /**
     * Returns an unmodifiable map of the sub profiles.
     *
     * @return the map
     */
    public Map<String, HashSet<UUID>> getSubProfiles() {
        return Map.copyOf(subProfiles);
    }

    /**
     * Retrieves a set of the sub profile names that a given sub profile has.
     * One single profile ID can be present multiple times, but only at different names.
     *
     * @param subProfile the sub profile ID to get the names from
     * @return the set of the names
     */
    public HashSet<String> getSubProfileNames(UUID subProfile) {
        HashSet<String> names = new HashSet<>();
        for (String key : subProfiles.keySet()) {
            if (subProfiles.get(key).contains(subProfile)) {
                names.add(key);
            }
        }
        return names;
    }

    /**
     * Adds a sub profile under the given name.
     * This automatically also adds the current profile as super profile in the sub profile.
     * The sub profile cannot be of type {@link ProfileType#NETWORK}.
     * If the sub profile is of type {@link ProfileType#SUB}, it cannot have a super profile yet.4
     *
     * @param subProfile the sub profile
     * @param name       the name
     * @return a {@link CompletableFuture} with a boolean: {@code true} if successful, otherwise {@code false}
     */
    public abstract CompletableFuture<Boolean> addSubProfile(ProfileData subProfile, String name);

    /**
     * Removes a sub profiler under the given name.
     * If the name is null, this will remove the sub profile from all its locations.
     * This automatically also removes the current profile as super profile from the sub profile.
     *
     * @param subProfile the sub profile
     * @param name       the name
     * @return a {@link CompletableFuture} with a boolean: {@code true} if successful, otherwise {@code false}
     */
    public abstract CompletableFuture<Boolean> removeSubProfile(ProfileData subProfile, String name);

    /**
     * Resets all data objects that are tied to a given profile.
     * This means that all data objects, but the profile information data object, identified by the "data" ID, are removed.
     * To also remove the profile information data object, use {@link #deleteProfile()}.
     *
     * @return a {@link CompletableFuture} with a void to signify success: all data objects are removed
     */
    public abstract CompletableFuture<Void> resetProfile();

    /**
     * Deletes the whole profile, this includes all data objects that are tied to it.
     * If the profile type is {@link ProfileType#NETWORK}, this can only be done when the profile has no super profile.
     *
     * @return a {@link CompletableFuture} with a void to signify success: the profile is completely deleted
     */
    public abstract CompletableFuture<Void> deleteProfile();

    /**
     * Copies the data objects (excluding the profile information data object, identified by the "data" ID) from the given profile to the current one.
     * This is not an exact duplicate, first run {@link #resetProfile()} in order to create an exact duplicate.
     * A copy of a data object has the same values besides its ID, read {@link DataManager#copyDataObject(PermissionKey, DataObjectId, DataObjectId)} for the exact behavior.
     *
     * @param from the profile to copy the data from
     * @return a {@link CompletableFuture} with a void to signify success: the profile is completely copied
     */
    public abstract CompletableFuture<Void> copyProfile(ProfileData from);

}
