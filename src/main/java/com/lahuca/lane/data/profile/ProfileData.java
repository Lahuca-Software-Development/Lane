package com.lahuca.lane.data.profile;

import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.data.RelationalId;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.records.ProfileRecord;
import com.lahuca.lane.records.RecordConverter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class ProfileData implements RecordConverter<ProfileRecord> { // TODO Could this also be an interface?!? Look at LaneParty and Retrieval!

    private final UUID id;
    private final ProfileType type;
    protected HashSet<UUID> superProfiles;
    /**
     * The key of the top map is the name of the sub profiles.
     * The sub profiles with that name are the keys of the value.
     * The value of the value determines whether the profile is active.
     */
    protected HashMap<String, HashMap<UUID, Boolean>> subProfiles;

    public ProfileData(ProfileRecord record) {
        this(record.id(), record.type(), record.superProfiles(), record.subProfiles());
    }

    public ProfileData(UUID id, ProfileType type, HashSet<UUID> superProfiles, HashMap<String, HashMap<UUID, Boolean>> subProfiles) {
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
     * Retrieves a sub profile with the given name and active state from this profile.
     * If it does not exist, it will be created.
     *
     * @param name   the name
     * @param active the active state
     * @return a {@link CompletableFuture} with the sub profile ID
     */
    public @NotNull CompletableFuture<UUID> fetchSubProfileId(@NotNull String name, boolean active) {
        Objects.requireNonNull(name, "name cannot be null");
        HashSet<UUID> subProfiles = getSubProfiles(name, active);
        if (subProfiles.isEmpty()) {
            return createSubProfile(ProfileType.SUB, name, active).thenApply(ProfileData::getId);
        }
        return CompletableFuture.completedFuture(subProfiles.iterator().next());
    }

    /**
     * Returns an unmodifiable map of the sub profiles.
     *
     * @return the map
     */
    public Map<String, HashMap<UUID, Boolean>> getSubProfiles() {
        return Map.copyOf(subProfiles);
    }

    /**
     * Returns a map of the sub profiles which are either active or inactive.
     *
     * @param active whether to look for active sub profiles
     * @return the map
     */
    public HashMap<String, HashSet<UUID>> getSubProfiles(boolean active) {
        HashMap<String, HashSet<UUID>> profiles = new HashMap<>();
        getSubProfiles().forEach((name, map) -> {
            HashSet<UUID> set = new HashSet<>();
            map.forEach((uuid, bool) -> {
                if (bool == active) set.add(uuid);
            });
            profiles.put(name, set);
        });
        return profiles;
    }

    /**
     * Returns a set of all sub profiles with the given name.
     *
     * @param name the name
     * @return the set
     */
    public Set<UUID> getSubProfiles(String name) {
        return getSubProfiles().getOrDefault(name, new HashMap<>()).keySet();
    }

    /**
     * Returns a set of all sub profiles that are active/inactive with the given name
     *
     * @param name   the name
     * @param active whether to look for active sub profiles
     * @return the set
     */
    public HashSet<UUID> getSubProfiles(String name, boolean active) {
        HashSet<UUID> set = new HashSet<>();
        getSubProfiles().getOrDefault(name, new HashMap<>()).forEach((uuid, bool) -> {
            if (bool == active) set.add(uuid);
        });
        return set;
    }

    /**
     * Retrieves a map of the sub profile names and active states that a given sub profile has.
     * One single profile ID can be present multiple times, but only at different names.
     * For every location (name) it has a boolean value to determine whether the sub profile is active in the current profile.
     *
     * @param subProfile the sub profile
     * @return a map with names and their active state
     */
    public HashMap<String, Boolean> getSubProfileData(UUID subProfile) {
        HashMap<String, Boolean> data = new HashMap<>();
        getSubProfiles().forEach((name, map) -> {
            Boolean active = map.getOrDefault(subProfile, null);
            if (active != null) data.put(name, active);
        });
        return data;
    }

    /**
     * Retrieves a set of the sub profile names that a given sub profile where it is active/inactive has.
     * One single profile ID can be present multiple times, but only at different names.
     * For every location (name) it has a boolean value to determine whether the sub profile is active in the current profile.
     *
     * @param subProfile the sub profile
     * @param active     whether to look for active sub profiles
     * @return a set with names
     */
    public HashSet<String> getSubProfileData(UUID subProfile, boolean active) {
        HashSet<String> data = new HashSet<>();
        getSubProfiles().forEach((name, map) -> {
            Boolean state = map.getOrDefault(subProfile, null);
            if (state != null && state == active) data.add(name);
        });
        return data;
    }

    /**
     * Creates a sub profile under the given name with the given active state.
     *
     * @param type   the profile type
     * @param name   the name
     * @param active if the sub profile is active
     * @return a {@link CompletableFuture} with the new profile data if successful
     */
    public abstract CompletableFuture<? extends ProfileData> createSubProfile(ProfileType type, String name, boolean active);

    /**
     * Adds a sub profile under the given name with the given active state.
     * This automatically also adds the current profile as super profile in the sub profile.
     * The sub profile cannot be of type {@link ProfileType#NETWORK}.
     * If the sub profile is of type {@link ProfileType#SUB}, it cannot have a super profile yet.
     * If the sub profile already exists are the given name, it still updates the active state.
     *
     * @param subProfile the sub profile
     * @param name       the name
     * @param active     if the sub profile is active
     * @return a {@link CompletableFuture} with a boolean: {@code true} if successful, otherwise {@code false}
     */
    public abstract CompletableFuture<Boolean> addSubProfile(ProfileData subProfile, String name, boolean active);

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

    @Override
    public String toString() {
        return new StringJoiner(", ", ProfileData.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("type=" + type)
                .add("superProfiles=" + superProfiles)
                .add("subProfiles=" + subProfiles)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileData that = (ProfileData) o;
        return id.equals(that.id) && type == that.type && superProfiles.equals(that.superProfiles) && subProfiles.equals(that.subProfiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, superProfiles, subProfiles);
    }

    @Override
    public ProfileRecord convertRecord() {
        return new ProfileRecord(id, type, superProfiles, subProfiles);
    }

}
