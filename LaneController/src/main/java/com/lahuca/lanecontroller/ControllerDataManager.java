package com.lahuca.lanecontroller;

import com.google.gson.Gson;
import com.lahuca.lane.connection.request.ResponseErrorException;
import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.data.RelationalId;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.data.manager.PermissionFailedException;
import com.lahuca.lane.data.profile.ProfileData;
import com.lahuca.lane.data.profile.ProfileType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ControllerDataManager {

    private final Controller controller;
    private final DataManager dataManager;
    private final Gson gson;

    public ControllerDataManager(Controller controller, DataManager dataManager, Gson gson) {
        this.controller = controller;
        this.dataManager = dataManager;
        this.gson = gson;
    }

    /**
     * Retrieves the data object at the given id with the given permission key.
     * When no data object exists at the given id, the optional is empty.
     * Otherwise, the data object is populated with the given data.
     * When the permission key does not grant reading, no information besides the id is filled in.
     * If the permission key is not an individual key, the completable future is thrown with an {@link IllegalArgumentException}.
     *
     * @param permissionKey the individual permission key to use while reading
     * @param id            the id of the data object to request
     * @return a completable future with an optional with the data object
     */
    public CompletableFuture<Optional<DataObject>> readDataObject(PermissionKey permissionKey, DataObjectId id) {
        if(!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.readDataObject(permissionKey, id);
    }

    /**
     * Writes the data object at the given id with the given permission key.
     * When no data object exists at the given id, it is created.
     * Otherwise, the data object is updated.
     * When the permission key does not grant writing, a {@link PermissionFailedException} is thrown in the {@link CompletableFuture}.
     * If the permission key is not an individual key, the completable future is thrown with an {@link IllegalArgumentException}.
     *
     * @param permissionKey the permission key to use while writing
     * @param object        the data object to update it with
     * @return a completable future with the void type to signify success: it has been written
     */
    public CompletableFuture<Void> writeDataObject(PermissionKey permissionKey, DataObject object) {
        if(!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.writeDataObject(permissionKey, object);
        // TODO We should check we do not overwrite info we reserve in defaultData.md
    }

    /**
     * Removes the data object at the given id with the given permission key.
     * When the permission key does not grant removing, it is not removed, and a {@link PermissionFailedException} is thrown in the {@link CompletableFuture}.
     * If the permission key is not an individual key, the completable future is thrown with an {@link IllegalArgumentException}.
     *
     * @param permissionKey the permission key to use while removing
     * @param id            the id of the data object to remove
     * @return a completable future with the void type to signify success: it was removed or did not exist
     */
    public CompletableFuture<Void> removeDataObject(PermissionKey permissionKey, DataObjectId id) {
        if(!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.removeDataObject(permissionKey, id);
    }

    /**
     * Updates the data object at the given id with the given permission key.
     * First the data object is read from the given id, then is accepted in the consumer.
     * The function can modify the values within the given data object.
     * After the consumer has been run, the updated data object is written back.
     * It is only written if the updater has returned true.
     * When the permission key does not grant writing, a {@link PermissionFailedException} is thrown in the {@link CompletableFuture}.
     * If the permission key is not an individual key, the completable future is thrown with an {@link IllegalArgumentException}.
     *
     * @param permissionKey the permission key to use while reading and writing
     * @param id            the id of the data object to update
     * @param updater       the updater consumer that handles the update
     * @return a completable future with the status as boolean: true if updated successfully or when the updater had returned false,
     * false when the data object did not exist.
     */
    public CompletableFuture<Boolean> updateDataObject(PermissionKey permissionKey, DataObjectId id, Function<DataObject, Boolean> updater) {
        if(!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.updateDataObject(permissionKey, id, updater);
    }

    /**
     * Retrieves a list of data object IDs whose key has the same prefix from the provided ID (case sensitive).
     * Example for the input with id = "myPrefix" with relationalId = ("players", "Laurenshup"), it will return:
     * <ul>
     *     <li>players.Laurenshup.myPrefix.value1</li>
     *     <li>players.Laurenshup.myPrefix.value2.subKey</li>
     *     <li>players.Laurenshup.myPrefixSuffix</li>
     * </ul>
     *
     * @param prefix the prefix ID. This cannot be null, its values can be null.
     * @return a {@link CompletableFuture} with the array of IDs with matching prefix
     */
    public CompletableFuture<ArrayList<DataObjectId>> listDataObjectIds(DataObjectId prefix) {
        return dataManager.listDataObjectIds(prefix);
    }

    /**
     * Copies a data object from one place to another.
     * This completely copies the data object, but replaces the ID.
     *
     * @param permissionKey the permission key to use while reading and writing
     * @param sourceId      the source data object ID
     * @param targetId      the target data object ID
     * @return a {@link CompletableFuture} with the void type to signify success: it has been copied
     */
    CompletableFuture<Void> copyDataObject(PermissionKey permissionKey, DataObjectId sourceId, DataObjectId targetId) {
        if(!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.copyDataObject(permissionKey, sourceId, targetId);
    }

    /**
     * Retrieves the profile data of the profile identified by the given UUID.
     *
     * @param uuid the profile's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will be the profile data if present
     */
    public CompletableFuture<Optional<ControllerProfileData>> getProfileData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        return dataManager.readDataObject(PermissionKey.CONTROLLER, new DataObjectId(RelationalId.Profiles(uuid), "data"))
                .thenApply(optDataObj -> optDataObj.flatMap(dataObj -> dataObj.getValueAsJson(gson, ControllerProfileData.class)));
    }

    /**
     * Creates a new profile given the profile type.
     * This stores the profile information at a new profile UUID.
     *
     * @param type the profile type
     * @return a {@link CompletableFuture} with a {@link UUID}, which is the UUID of the new profile
     */
    public CompletableFuture<ControllerProfileData> createNewProfile(@NotNull ProfileType type) {
        Objects.requireNonNull(type, "type cannot be null");
        UUID uuid = UUID.randomUUID();
        return getProfileData(uuid).thenCompose(optProfile -> {
            if(optProfile.isPresent()) return createNewProfile(type);
            // Our UUID is unique, now create
            // TODO This can still cause troubles! We should do reservation: write into reservation array somewhere, even before getProfileData!
            ControllerProfileData newData = new ControllerProfileData(uuid, type);
            DataObject dataObject = new DataObject(newData.getDataObjectId(), PermissionKey.CONTROLLER, gson, newData);
            return dataManager.writeDataObject(PermissionKey.CONTROLLER, dataObject).thenApply(none -> newData);
        });
    }

    /**
     * Creates a sub profile to another "super profile", the current profile, at the given name.
     * This returns a {@link CompletableFuture} with the profile that has been made and added to the super profile.
     * Internally, first creates a new profile;
     * after which the current profile is added as super profile in the sub profile;
     * then the sub profile is added to the current profile.
     * These changes are reflected in the respective parameters' values as well.
     * The type cannot be of type {@link ProfileType#NETWORK}.
     *
     * @param current the current profile, where to create the sub profile
     * @param type    the profile type to create
     * @param name    the name to add the sub profile to
     * @param active  whether the sub profile is active
     * @return a {@link CompletableFuture} with the new profile data if successful
     */
    public @NotNull CompletableFuture<ControllerProfileData> createSubProfile(@NotNull ControllerProfileData current, @NotNull ProfileType type, @NotNull String name, boolean active) {
        return createNewProfile(type).thenCompose(subProfile -> addSubProfile(current, subProfile, name, active).thenCompose(status -> {
            if(!status) {
                resetDeleteProfile(subProfile, true);
                return CompletableFuture.failedFuture(new IllegalStateException("Could not add the newly created sub profile to the current profile"));
            }
            return CompletableFuture.completedFuture(subProfile);
        }));
    }

    /**
     * Adds a sub profile to another "super profile", the current profile at the given name.
     * This returns a {@link CompletableFuture} with the result whether the sub profile has been added or not.
     * Internally, first adds the current profile as super profile in the sub profile;
     * after which the sub profile is added to the current profile.
     * These changes are reflected in the respective parameters' values as well.
     * The sub profile cannot be of type {@link ProfileType#NETWORK}.
     * If the sub profile is of type {@link ProfileType#SUB}, it cannot have a super profile yet.
     * If the sub profile already exists at the given name and profile, it still updates the active state.
     *
     * @param current    the current profile, where to add the sub profile to
     * @param subProfile the sub profile to add
     * @param name       the name to add the sub profile to
     * @param active     whether the sub profile is active
     * @return a {@link CompletableFuture} with a boolean: {@code true} if the sub profile has been added, {@code false} otherwise
     */
    public CompletableFuture<Boolean> addSubProfile(ControllerProfileData current, ControllerProfileData subProfile, String name, boolean active) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(subProfile, "subProfile cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        if(subProfile.getType() == ProfileType.NETWORK) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot add a network profile as sub profile"));
        }
        if(subProfile.getType() == ProfileType.SUB && !subProfile.getSuperProfiles().isEmpty()) {
            // Check whether we update the inactive/active state
            if(!current.getSubProfileData(subProfile.getId()).containsKey(name)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("This sub profile already has a super profile which is not the current profile and current name"));
            }
        }

        // First we update the sub profile so that it holds the super profile.
        return dataManager.updateDataObject(PermissionKey.CONTROLLER, subProfile.getDataObjectId(), obj -> {
            subProfile.addSuperProfile(current.getId());
            obj.setValue(gson, subProfile);
            return true;
        }).thenCompose(status -> {
            if(!status) {
                return CompletableFuture.failedFuture(new IllegalStateException("Profile data of sub profile did not exist"));
            }
            // We know we updated the sub profile, now update the current one.
            return dataManager.updateDataObject(PermissionKey.CONTROLLER, current.getDataObjectId(), obj -> {
                current.addSubProfile(subProfile.getId(), name, active);
                obj.setValue(gson, current);
                return true;
            });
        });
    }

    /**
     * Removes a sub profile from another "super profile", the current profile at the given name.
     * If the name is null, the sub profile is removed at all positions in the super profile.
     * This returns a {@link CompletableFuture} with the result whether the sub profile has been removed or not.
     * Internally, first removes the sub profile from the current profile;
     * after which the super profile is removed from the sub profile.
     * These changes are reflected in the respective parameters' values as well.
     *
     * @param current    the current profile, where to remove the sub profile from
     * @param subProfile the sub profile to remove
     * @param name       the name to remove the sub profile from, or null to completely remove it
     * @return a {@link CompletableFuture} with a boolean: {@code true} if the sub profile has been removed, {@code false} otherwise
     */
    public CompletableFuture<Boolean> removeSubProfile(ControllerProfileData current, ControllerProfileData subProfile, String name) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(subProfile, "subProfile cannot be null");

        // First we update the super profile so that it does not hold the sub profile.
        return dataManager.updateDataObject(PermissionKey.CONTROLLER, current.getDataObjectId(), obj -> {
            current.removeSubProfile(subProfile.getId(), name);
            obj.setValue(gson, current);
            return true;
        }).thenCompose(status -> {
            if(!status) return CompletableFuture.completedFuture(false);
            // We know we updated the current profile, now update the sub profile.
            if(!current.getSubProfileData(subProfile.getId()).isEmpty()) {
                // We do not need to update, it is still a super profile
                return CompletableFuture.completedFuture(true);
            }
            return dataManager.updateDataObject(PermissionKey.CONTROLLER, subProfile.getDataObjectId(), obj -> {
                subProfile.removeSuperProfile(current.getId());
                obj.setValue(gson, subProfile);
                return true;
            });
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
    public CompletableFuture<Void> resetDeleteProfile(ControllerProfileData current, boolean delete) {
        Objects.requireNonNull(current, "current cannot be null");
        if(delete && (!current.getSuperProfiles().isEmpty() || !current.getSubProfiles().isEmpty())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot delete profile with sub or super profiles"));
        }
        return dataManager.listDataObjectIds(new DataObjectId(RelationalId.Profiles(current.getId()), null)).thenCompose(dataObjectIds -> {
            if(!delete)
                dataObjectIds.remove(current.getDataObjectId()); // Remove the data object information if deleting
            // Create futures that remove every one of them
            CompletableFuture<?>[] futures = new CompletableFuture[dataObjectIds.size()];
            for(int i = 0; i < dataObjectIds.size(); i++) {
                futures[i] = dataManager.removeDataObject(PermissionKey.CONTROLLER, dataObjectIds.get(i));
            }
            // Combine the futures into one that only accepts if all of them do
            return CompletableFuture.allOf(futures);
        }).exceptionally(val -> {
            val.printStackTrace();
            return null;
        });
    }

    /**
     * Copies one profile to another, see {@link ProfileData#copyProfile(ProfileData)}, this does not copy the profile data information object.
     *
     * @param current the profile to copy to
     * @param from    the profile to copy from
     * @return a {@link CompletableFuture} with a void to signify success: it has been copied completely
     */
    public CompletableFuture<Void> copyProfile(ControllerProfileData current, ProfileData from) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(from, "from cannot be null");
        return dataManager.listDataObjectIds(new DataObjectId(RelationalId.Profiles(from.getId()), null)).thenCompose(dataObjectIds -> {
            dataObjectIds.remove(from.getDataObjectId()); // Remove the data object information
            // Create futures that copy every one of them
            CompletableFuture<?>[] futures = new CompletableFuture[dataObjectIds.size()];
            for(int i = 0; i < dataObjectIds.size(); i++) {
                DataObjectId fromDataObject = dataObjectIds.get(i);
                DataObjectId toDataObject = new DataObjectId(RelationalId.Profiles(current.getId()), fromDataObject.id());
                System.out.println("Copying " + fromDataObject + " to " + toDataObject);
                futures[i] = dataManager.copyDataObject(PermissionKey.CONTROLLER, fromDataObject, toDataObject);
            }
            // Combine the futures into one that only accepts if all of them do
            return CompletableFuture.allOf(futures);
        });
    }

    /**
     * Sets the network profile of the given player to the provided profile.
     * The profile must be of type {@link ProfileType#NETWORK}.
     *
     * @param player  the player
     * @param profile the profile
     * @return a {@link CompletableFuture} to signify success: the profile has been set
     */
    public CompletableFuture<Void> setNetworkProfile(ControllerPlayer player, ControllerProfileData profile) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        // Check if already set
        if(player.getNetworkProfileUuid().equals(profile.getId())) return CompletableFuture.completedFuture(null);
        // Check whether the profile can be set
        if(profile.getType() != ProfileType.NETWORK || !profile.getSuperProfiles().isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Can only set a network profile with no super profiles"));
        }
        // Retrieve old profile
        return player.getNetworkProfile().thenCompose(oldProfile -> {
            // Update profiles, first we update the sub profile so that it holds the super profile.
            return dataManager.updateDataObject(PermissionKey.CONTROLLER, profile.getDataObjectId(), obj -> {
                // TODO What if we could not continue, this would be set. And the sub profile has multiple super profiles!!!!
                //  Check this at all locations.
                profile.addSuperProfile(player.getUuid());
                obj.setValue(gson, profile);
                return true;
            }).thenCompose(status -> {
                if(status) {
                    // We added the super profile. Now replace the network profile
                    return DefaultDataObjects.setPlayersNetworkProfile(dataManager, player.getUuid(), profile.getId())
                            .thenCompose(data -> {
                                // We can remove the super profile from the original one
                                return dataManager.updateDataObject(PermissionKey.CONTROLLER, oldProfile.getDataObjectId(), obj -> {
                                    oldProfile.removeSuperProfile(player.getUuid());
                                    obj.setValue(gson, oldProfile);
                                    return true;
                                }).thenAccept(status2 -> {
                                    // Super profile has been removed from the original one
                                    // Super profile from new one is the player, player has the new one. Just object object
                                    player.setNetworkProfileUuid(profile.getId());
                                });
                            });
                }
                return CompletableFuture.failedFuture(new ResponseErrorException("Could not set super profile to new profile"));
            });
        });
    }

    /**
     * Sets a network profile, as if the player did not have one currently.
     * This is an internal function for the Controller when a player has joined the network newly.
     *
     * @param player  the player's UUID
     * @param profile the profile
     * @return a {@link CompletableFuture} with a void to signify succes: the new profile has been set
     */
    CompletableFuture<Void> setNewNetworkProfile(UUID player, ControllerProfileData profile) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        // Check whether the profile can be set
        if(profile.getType() != ProfileType.NETWORK || !profile.getSuperProfiles().isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Can only set a network profile with no super profiles"));
        }
        // Update profiles, first we update the sub profile so that it holds the super profile.
        return dataManager.updateDataObject(PermissionKey.CONTROLLER, profile.getDataObjectId(), obj -> {
            // TODO What if we could not continue, this would be set. And the sub profile has multiple super profiles!!!!
            //  Check this at all locations.
            profile.addSuperProfile(player);
            obj.setValue(gson, profile);
            return true;
        }).thenCompose(status -> {
            if(status) {
                // We added the super profile. Now replace the network profile
                return DefaultDataObjects.setPlayersNetworkProfile(dataManager, player, profile.getId());
            }
            return CompletableFuture.failedFuture(new ResponseErrorException("Could not set super profile to new profile"));
        });
    }

}
