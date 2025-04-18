package com.lahuca.lane.data.manager;

import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface DataManager {

    /**
     * Shutdown the data manager.
     */
    void shutdown();

    // TODO Do PermissionException instead!

    /**
     * Retrieves the data object at the given id with the given permission key.
     * When no data object exists at the given id: the optional is empty.
     * Otherwise, the data object is populated with the given data.
     * When the permission key does not grant reading, no information besides the id is filled in.
     * The {@link CompletableFuture} might contain exceptions.
     *
     * @param permissionKey the permission key to use while reading
     * @param id            the id of the data object to request
     * @return a completable future with an optional with the data object
     */
    CompletableFuture<Optional<DataObject>> readDataObject(PermissionKey permissionKey, DataObjectId id);

    /**
     * Writes the data object at the given id with the given permission key.
     * When no data object exists at the given id, it is created.
     * Otherwise, the data object is updated.
     * When the permission key does not grant writing, a {@link PermissionFailedException} is thrown in the {@link CompletableFuture}.
     *
     * @param permissionKey the permission key to use while writing
     * @param object        the data object to update it with
     * @return a completable future with the void type to signify success: it has been written
     */
    CompletableFuture<Void> writeDataObject(PermissionKey permissionKey, DataObject object);

    /**
     * Removes the data object at the given id with the given permission key.
     * When the permission key does not grant removing, it is not removed, and a {@link PermissionFailedException} is thrown in the {@link CompletableFuture}.
     *
     * @param permissionKey the permission key to use while removing
     * @param id            the id of the data object to remove
     * @return a completable future with the void type to signify success: it was removed or did not exist
     */
    CompletableFuture<Void> removeDataObject(PermissionKey permissionKey, DataObjectId id);

    /**
     * Updates the data object at the given id with the given permission key.
     * First the data object is read from the given id, then is accepted in the consumer.
     * The function can modify the values within the given data object.
     * After the consumer has been run, the updated data object is written back.
     * It is only written if the updater has returned true.
     * When the permission key does not grant writing, a {@link PermissionFailedException} is thrown in the {@link CompletableFuture}.
     *
     * @param permissionKey the permission key to use while reading and writing
     * @param id            the id of the data object to update
     * @param updater       the updater consumer that handles the update
     * @return a completable future with the status as boolean: true if updated successfully or when the updater had returned false,
     * false when the data object did not exist.
     */
    default CompletableFuture<Boolean> updateDataObject(PermissionKey permissionKey, DataObjectId id, Function<DataObject, Boolean> updater) {
        return readDataObject(permissionKey, id).thenCompose(dataObjectOptional -> {
            if (dataObjectOptional.isEmpty()) return CompletableFuture.completedFuture(false);
            DataObject dataObject = dataObjectOptional.get();
            boolean write = updater.apply(dataObject);
            return write ? writeDataObject(permissionKey, dataObject).thenApply(v -> true) : CompletableFuture.completedFuture(true);
        });
    }


}
