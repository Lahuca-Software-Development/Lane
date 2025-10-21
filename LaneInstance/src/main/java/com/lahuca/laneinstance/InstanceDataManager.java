package com.lahuca.laneinstance;

import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.packet.ProfilePacket;
import com.lahuca.lane.connection.packet.data.*;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.UnsuccessfulResultException;
import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.data.profile.ProfileData;
import com.lahuca.lane.data.profile.ProfileType;
import com.lahuca.lane.records.ProfileRecord;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class InstanceDataManager {

    private final LaneInstance instance;

    // TODO id() == null check everywhere!

    public InstanceDataManager(LaneInstance instance) {
        this.instance = instance;
    }

    public String id() {
        return instance.getId();
    }

    public Connection connection() {
        return instance.getConnection();
    }

    private static <T> CompletableFuture<T> simpleException(String result) { // TODO Move
        return CompletableFuture.failedFuture(new UnsuccessfulResultException(result));
    }

    /**
     * Reads a data object at the given id with the provided permission key.
     *
     * @param id            the id of the data object
     * @param permissionKey the permission key that wants to retrieve the data object, this must be an individual key
     * @return a {@link CompletableFuture} with a {@link Optional} with the data object as value if present
     */
    public CompletableFuture<Optional<DataObject>> readDataObject(DataObjectId id, PermissionKey permissionKey) {
        if (id() == null || id == null || permissionKey == null || !permissionKey.isFormattedCorrectly())
            return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection().<DataObject>sendRequestPacket(requestId -> new DataObjectReadPacket(requestId, id, permissionKey), null).getResult().thenApply(Optional::ofNullable);
    }

    /**
     * Writes a data object at the given id with the provided permission key.
     * This either creates or updates the data object.
     *
     * @param object        the id of the data object
     * @param permissionKey the permission key that wants to write the data object, this must be an individual key
     * @return a {@link CompletableFuture} with a void to signify success: the data object has been written
     */
    public CompletableFuture<Void> writeDataObject(DataObject object, PermissionKey permissionKey) {
        if (id() == null || object == null || permissionKey == null || !permissionKey.isFormattedCorrectly())
            return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection().<Void>sendRequestPacket(requestId -> new DataObjectWritePacket(requestId, object, permissionKey), null).getResult();
    }

    /**
     * Removes a data object at the given id with the provided permission key.
     *
     * @param id            the id of the data object
     * @param permissionKey the permission key that wants to remove the data object, this must be an individual key
     * @return a {@link CompletableFuture} with a void to signify success: the data object has been removed
     */
    public CompletableFuture<Void> removeDataObject(DataObjectId id, PermissionKey permissionKey) {
        if (id() == null || id == null || permissionKey == null || !permissionKey.isFormattedCorrectly())
            return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection().<Void>sendRequestPacket(requestId -> new DataObjectRemovePacket(requestId, id, permissionKey), null).getResult();
    }

    // TODO updateDataObject

    /**
     * Retrieves a list of data object IDs whose key has the same prefix from the provided ID (case sensitive).
     * Example for the input with id = "myPrefix" with relationalId = ("players", "Laurenshup"), it will return:
     * <ul>
     *     <li>players.Laurenshup.myPrefix.value1</li>
     *     <li>players.Laurenshup.myPrefix.value2.subKey</li>
     *     <li>players.Laurenshup.myPrefixSuffix</li>
     * </ul>
     * @param prefix the prefix ID. This cannot be null, its values can be null.
     * @return a {@link CompletableFuture} with the array of IDs with matching prefix
     */
    public @NotNull CompletableFuture<ArrayList<DataObjectId>> listDataObjectIds(DataObjectId prefix) {
        if(id() == null || prefix == null) return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection().<ArrayList<DataObjectId>>sendRequestPacket(requestId -> new DataObjectListIdsPacket(requestId, prefix), null).getResult();
    }

    /**
     * Retrieves a list of DataObjects from the given table that match the version.
     *
     * @param prefix        the prefix ID. This cannot be null, its values can be null.
     * @param permissionKey the permission key to use while reading and writing
     * @param version       the version to match, null if no version is required
     * @return a {@link CompletableFuture} with the array of DataObjects matching the version
     */
    public @NotNull CompletableFuture<ArrayList<DataObject>> listDataObjects(@NotNull DataObjectId prefix, PermissionKey permissionKey, Integer version) {
        if(id() == null || prefix == null || permissionKey == null || !permissionKey.isFormattedCorrectly()) return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection().<ArrayList<DataObject>>sendRequestPacket(requestId -> new DataObjectsListPacket(requestId, prefix, permissionKey, version), null).getResult();
    }

    /**
     * Copies a data object from one place to another.
     * This completely copies the data object, but replaces the ID.
     * @param permissionKey the permission key to use while reading and writing
     * @param sourceId the source data object ID
     * @param targetId the target data object ID
     * @return a {@link CompletableFuture} with the void type to signify success: it has been copied
     */
    CompletableFuture<Void> copyDataObject(PermissionKey permissionKey, DataObjectId sourceId, DataObjectId targetId) {
        if (id() == null || permissionKey == null || !permissionKey.isFormattedCorrectly() || sourceId == null || targetId == null) {
            return simpleException(ResponsePacket.INVALID_PARAMETERS);
        }
        return connection().<Void>sendRequestPacket(requestId -> new DataObjectCopyPacket(requestId, permissionKey, sourceId, targetId), null).getResult();
    }

    /**
     * Retrieves the profile data of the profile identified by the given UUID.
     * @param uuid the profile's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will be the profile data if present
     */
    public CompletableFuture<Optional<InstanceProfileData>> getProfileData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        return connection().<ProfileRecord>sendRequestPacket(id -> new ProfilePacket.GetProfileData(id, uuid), null).getResult()
                .thenApply(Optional::ofNullable).thenApply(opt -> opt.map(InstanceProfileData::new));
    }

    /**
     * Creates a new profile given the profile type.
     * This stores the profile information at a new profile UUID.
     * @param type the profile type
     * @return a {@link CompletableFuture} with a {@link UUID}, which is the UUID of the new profile
     */
    public CompletableFuture<InstanceProfileData> createNewProfile(ProfileType type) {
        Objects.requireNonNull(type, "type cannot be null");
        return connection().<ProfileRecord>sendRequestPacket(id -> new ProfilePacket.CreateNew(id, type), null).getResult().thenApply(InstanceProfileData::new);
    }

    /**
     * Creates a sub profile to another "super profile", the current profile, at the given name.
     * This returns a {@link CompletableFuture} with the profile that has been made and added to the super profile.
     * Internally, first creates a new profile;
     * after which the current profile is added as super profile in the sub profile;
     * then the sub profile is added to the current profile.
     * These changes are reflected in the respective parameters' values as well.
     * The type cannot be of type {@link ProfileType#NETWORK}.
     * @param current the current profile, where to create the sub profile
     * @param type the profile type to create
     * @param name the name to add the sub profile to
     * @param active whether the sub profile is active
     * @return a {@link CompletableFuture} with the new profile data if successful
     */
    public @NotNull CompletableFuture<InstanceProfileData> createSubProfile(@NotNull InstanceProfileData current, @NotNull ProfileType type, @NotNull String name, boolean active) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        if (type == ProfileType.NETWORK) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot add a network profile as sub profile"));
        }
        return connection().<ProfileRecord>sendRequestPacket(id -> new ProfilePacket.CreateSubProfile(id, current.getId(), type, name, active), null).getResult()
                .thenApply(profile -> {
                    if(profile != null) {
                        current.addSubProfile(profile.id(), name, active);
                    }
                    return new InstanceProfileData(profile);
                });
    }

    /**
     * Adds a sub profile to another "super profile", the current profile at the given name with the given active state.
     * This returns a {@link CompletableFuture} with the result whether the sub profile has been added or not.
     * These changes are reflected in the respective parameters' values as well; due to the implementation, this might not be the most up-to-date data.
     * The sub profile cannot be of type {@link ProfileType#NETWORK}.
     * If the sub profile is of type {@link ProfileType#SUB}, it cannot have a super profile yet.
     * If the sub profile already exists at the given name and profile, it still updates the active state.
     *
     * @param current    the current profile, where to add the sub profile to
     * @param subProfile the sub profile to add
     * @param name       the name to add the sub profile to
     * @param active whether the sub profile is active
     * @return a {@link CompletableFuture} with a boolean: {@code true} if the sub profile has been added, {@code false} otherwise
     */
    public CompletableFuture<Boolean> addSubProfile(InstanceProfileData current, InstanceProfileData subProfile, String name, boolean active) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(subProfile, "subProfile cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        if (subProfile.getType() == ProfileType.NETWORK) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot add a network profile as sub profile"));
        }
        if (subProfile.getType() == ProfileType.SUB && !subProfile.getSuperProfiles().isEmpty()) {
            // Check whether we update the inactive/active state
            if(!current.getSubProfileData(subProfile.getId()).containsKey(name)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("This sub profile already has a super profile which is not the current profile and current name"));
            }
        }
        return connection().<Boolean>sendRequestPacket(id -> new ProfilePacket.AddSubProfile(id, current.getId(), subProfile.getId(), name, active), null).getResult()
                .thenApply(status -> {
                    if(status) {
                        subProfile.addSuperProfile(current.getId());
                        current.addSubProfile(subProfile.getId(), name, active);
                    }
                    return status;
                });
    }

    /**
     * Removes a sub profile from another "super profile", the current profile at the given name.
     * If the name is null, the sub profile is removed at all positions in the super profile.
     * This returns a {@link CompletableFuture} with the result whether the sub profile has been removed or not.
     * These changes are reflected in the respective parameters' values as well; due to the implementation, this might not be the most up-to-date data.
     *
     * @param current    the current profile, where to remove the sub profile from
     * @param subProfile the sub profile to remove
     * @param name       the name to remove the sub profile from, or null to completely remove it
     * @return a {@link CompletableFuture} with a boolean: {@code true} if the sub profile has been removed, {@code false} otherwise
     */
    public CompletableFuture<Boolean> removeSubProfile(InstanceProfileData current, InstanceProfileData subProfile, String name) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(subProfile, "subProfile cannot be null");
        return connection().<Boolean>sendRequestPacket(id -> new ProfilePacket.RemoveSubProfile(id, current.getId(), subProfile.getId(), name), null).getResult()
                .thenApply(status -> {
                    if(status) {
                        current.removeSubProfile(subProfile.getId(), name);
                        subProfile.removeSuperProfile(current.getId());
                    }
                    return status;
                });
    }

    /**
     * Resets or deletes the profile, see {@link ProfileData#resetProfile()} and {@link ProfileData#deleteProfile()}.
     * This removes all data objects for the specified profile, when only resetting, this leaves the profile data intact.
     * If the profile type is {@link ProfileType#NETWORK}, this can only be done when the profile has no super profile.
     *
     * @param current the profile
     * @param delete  whether to also remove the profile data info
     * @return a {@link CompletableFuture} with a void to signify success: it has been reset/deleted completely
     */
    public CompletableFuture<Void> resetDeleteProfile(InstanceProfileData current, boolean delete) {
        Objects.requireNonNull(current, "current cannot be null");
        if (delete && (!current.getSuperProfiles().isEmpty() || !current.getSubProfiles().isEmpty())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot delete profile with sub or super profiles"));
        }
        return connection().<Void>sendRequestPacket(id -> new ProfilePacket.ResetDelete(id, current.getId(), delete), null).getResult();
    }

    /**
     * Copies one profile to another, see {@link ProfileData#copyProfile(ProfileData)}, this does not copy the profile data information object.
     *
     * @param current the profile to copy to
     * @param from the profile to copy from
     * @return a {@link CompletableFuture} with a void to signify success: it has been copied completely
     */
    public CompletableFuture<Void> copyProfile(InstanceProfileData current, ProfileData from) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(from, "from cannot be null");
        return connection().<Void>sendRequestPacket(id -> new ProfilePacket.Copy(id, current.getId(), from.getId()), null).getResult();
    }

    /**
     * Sets the network profile of the given player to the provided profile.
     * The profile must be of type {@link ProfileType#NETWORK}.
     *
     * @param player the player
     * @param profile the profile
     * @return a {@link CompletableFuture} to signify success: the profile has been set
     */
    public CompletableFuture<Void> setNetworkProfile(InstancePlayer player, InstanceProfileData profile) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        // Check if already set
        if(player.getNetworkProfileUuid().equals(profile.getId())) return CompletableFuture.completedFuture(null);
        // Check whether the profile can be set
        if(profile.getType() != ProfileType.NETWORK || !profile.getSuperProfiles().isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Can only set a network profile with no super profiles"));
        }
        return connection().<Void>sendRequestPacket(id -> new ProfilePacket.SetNetworkProfile(id, player.getUuid(), profile.getId()), null).getResult();
    }

}
