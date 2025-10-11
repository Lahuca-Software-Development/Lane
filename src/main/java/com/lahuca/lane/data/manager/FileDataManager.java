package com.lahuca.lane.data.manager;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Data Manager that uses the file system:
 * <ul>
 *     <li>/data</li>
 *     <ul>
 *         <li>/singular</li>
 *         <ul>
 *             <li>/main.key.json</li>
 *             <li>/main.key2.json</li>
 *             <li>/main2.sub.key</li>
 *         </ul>
 *         <li>/relational</li>
 *         <ul>
 *             <li>/players</li>
 *             <ul>
 *                 <li>/e8cfb5f7-3505-4bd5-b9c0-5ca9a6967daa</li>
 *                 <ul>
 *                     <li>/locale.json</li>
 *                     <li>/main.key.json</li>
 *                 </ul>
 *             </ul>
 *         </ul>
 *     </ul>
 * </ul>
 * The JSON files themselves are the data objects.
 */
public class FileDataManager implements DataManager {

    // TODO Make method that runs through all data objects in the system to remove any that are supposed to be gone due to removalTime. This should spare data.

    private final Gson gson;
    private final File dataFolder;
    private final long startTime = System.currentTimeMillis();
    private final HashSet<DataObjectId> removeOnStop = new HashSet<>(); // TODO Maybe too much RAM usage?

    public FileDataManager(Gson gson, File dataFolder) throws FileNotFoundException {
        this.gson = gson;
        this.dataFolder = dataFolder;
        if(!dataFolder.exists()) {
            if(!dataFolder.mkdirs()) {
                throw new FileNotFoundException("Unable to create data folder, could not find file");
            }
        }
    }

    private File buildFilePath(DataObjectId id) {
        File file;
        if(id.isRelational()) file = new File(dataFolder, "relational" + File.separator + id.relationalId().type() + File.separator + id.relationalId().id());
        else file = new File(dataFolder, "singular");
        return new File(file, id.id() + ".json");
    }

    @Override
    public void shutdown() {
        removeOnStop.forEach(id -> removeDataObject(PermissionKey.CONTROLLER, id));
    }

    @Override
    public CompletableFuture<Optional<DataObject>> readDataObject(PermissionKey permissionKey, DataObjectId id) {
        File file = buildFilePath(id);
        if(!file.exists()) return CompletableFuture.completedFuture(Optional.empty());
        try (FileReader reader = new FileReader(file)) {
            DataObject object = gson.fromJson(reader, DataObject.class);
            // First check if this object is to be removed
            if(object.shouldRemove(startTime)) {
                return removeDataObject(PermissionKey.CONTROLLER, id).thenApply(status -> Optional.empty());
            }
            // Object should not be removed, check read access
            boolean readAccess = object.hasReadAccess(permissionKey, true);
            boolean writeAccess = object.hasWriteAccess(permissionKey, false);
            object = object.shallowCopy(null, readAccess, writeAccess);
            return CompletableFuture.completedFuture(Optional.of(object));
        } catch (IOException | JsonIOException | JsonSyntaxException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Void> writeDataObject(PermissionKey permissionKey, DataObject object) {
        // Check if given object is even valid.
        if(!object.isWriteable()) return CompletableFuture.failedFuture(new IllegalArgumentException("Object is not writeable"));
        if(!object.hasWriteAccess(permissionKey, false)) return CompletableFuture.failedFuture(new PermissionFailedException("Permission key does not allow writing given object"));
        File file = buildFilePath(object.getId());
        // Check if it already exists
        if(!file.exists()) {
            // It does not exist, create it first.
            try {
                if(!file.getParentFile().exists()) {
                    if(!file.getParentFile().mkdirs()) return CompletableFuture.failedFuture(new SecurityException("Could not create parent directory"));
                }
                if(!file.createNewFile()) return CompletableFuture.failedFuture(new SecurityException("Could not create file"));
            } catch (IOException | SecurityException e) {
                return CompletableFuture.failedFuture(e);
            }
        } else {
            // It exists, check if we have write access on it.
            try (FileReader reader = new FileReader(file)) {
                DataObject current = gson.fromJson(reader, DataObject.class);
                boolean writeAccess = current.hasWriteAccess(permissionKey, false);
                if(!writeAccess) return CompletableFuture.failedFuture(new PermissionFailedException("Permission key does not allow writing saved object"));
            } catch (IOException | JsonIOException | JsonSyntaxException | SecurityException e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        // We can overwrite, update last update first
        object.setLastUpdated(System.currentTimeMillis());
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(object, writer);
            // It is completed, if removed upon stop, add to list
            object.getRemovalTime().ifPresent(removalTime -> {
                if(removalTime == 0) {
                    removeOnStop.add(object.getId());
                } else {
                    removeOnStop.remove(object.getId());
                }
            });
            return CompletableFuture.completedFuture(null);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Void> removeDataObject(PermissionKey permissionKey, DataObjectId id) {
        File file = buildFilePath(id);
        if(!file.exists()) return CompletableFuture.completedFuture(null);
        try (FileReader reader = new FileReader(file)) {
            DataObject object = gson.fromJson(reader, DataObject.class);
            boolean writeAccess = object.hasWriteAccess(permissionKey, false);
            if(!writeAccess) return CompletableFuture.failedFuture(new PermissionFailedException("Permission key does not allow removing saved object"));
            else {
                // Remove
                if(file.delete()) {
                    // Remove the parent directory for if this is empty
                    if(Optional.ofNullable(file.getParentFile().listFiles()).map(files -> files.length == 0).orElse(false)) {
                        file.getParentFile().delete();
                    }
                    return CompletableFuture.completedFuture(null);
                }
                else return CompletableFuture.failedFuture(new SecurityException("Could not delete file"));
            }
        } catch (IOException | JsonIOException | JsonSyntaxException | SecurityException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<ArrayList<DataObjectId>> listDataObjectIds(DataObjectId prefix) {
        Objects.requireNonNull(prefix, "prefix cannot be null");
        File folder;
        if(prefix.isRelational()) folder = new File(dataFolder, "relational" + File.separator + prefix.relationalId().type() + File.separator + prefix.relationalId().id());
        else folder = new File(dataFolder, "singular");
        String keyPrefix = prefix.id();
        if(keyPrefix == null) keyPrefix = "";
        ArrayList<DataObjectId> ids = new ArrayList<>();
        File[] files = folder.listFiles();
        if(files == null) return CompletableFuture.completedFuture(new ArrayList<>());
        for (File file : files) {
            String nameExtension = file.getName();
            String name = nameExtension;
            if(nameExtension.contains(".")) {
                name = name.substring(0, nameExtension.lastIndexOf("."));
            }
            if(name.startsWith(keyPrefix)) {
                ids.add(new DataObjectId(prefix.relationalId(), name));
            }
        }
        return CompletableFuture.completedFuture(ids);
    }

}
