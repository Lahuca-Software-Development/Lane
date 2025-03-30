package com.lahuca.lane.data.manager;

import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DataManager {

    /**
     * Shutdown the data manager.
     */
    void shutdown();

    /**
     * Retrieves the data object at the given id with the given permission key.
     * When no data object exists at the given id, the optional is empty; or if it cannot be retrieved.
     * Otherwise, the data object is populated with the given data.
     * When the permission key does not grant reading, no information besides the id is filled in.
     * @param permissionKey the permission key to use while reading
     * @param id the id of the data object to request
     * @return a completable future with an optional with the data object
     */
    CompletableFuture<Optional<DataObject>> readDataObject(PermissionKey permissionKey, DataObjectId id);

    /**
     * Writes the data object at the given id with the given permission key.
     * When no data object exists at the given id, it is created.
     * Otherwise, the data object is updated.
     * When the permission key does not grant writing, it is not updated.
     * @param permissionKey the permission key to use while writing
     * @param object the data object to update it with
     * @return a completable future with the status as boolean: true if successful or false if not enough permission
     */
    CompletableFuture<Boolean> writeDataObject(PermissionKey permissionKey, DataObject object);

    /**
     * Removes the data object at the given id with the given permission key.
     * When the permission key does not grant removing, it is not removed.
     * @param permissionKey the permission key to use while removing
     * @param id the id of the data object to remove
     * @return a completable future with the status as boolean: true if successful or did not exist or false if not enough permission
     */
    CompletableFuture<Boolean> removeDataObject(PermissionKey permissionKey, DataObjectId id);



}
