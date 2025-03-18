package com.lahuca.lane.data;

import com.google.gson.Gson;

import java.util.Collection;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The class for data objects.
 * Data objects are either created by a plugin/module and registered in the controller,
 * or by retrieving it from the controller.
 * Be aware, that due to the permissions it can happen that some values are null:
 * <ul>
 *     <li>The permission keys are null whenever the requester does not have that permission.</li>
 *     <li>The removal time and value data are null whenever the requester does not have the read permission.</li>
 * </ul>
 *
 * The values are internally stored as {@link String},
 * therefore be aware that when using the constructor with the type and value {@link Object#toString()} is used.<br>
 * For example when providing {@link DataObjectType#INTEGER} together with a {@link Long} will cause problems when converting in some storage systems.
 * This will result in information loss or other incorrect behaviour.<br>
 * The types {@link DataObjectType#DATE}, {@link DataObjectType#TIME} and {@link DataObjectType#TIMESTAMP} require a long as input.
 * This long is defined by using the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.<br>
 * The type {@link DataObjectType#ARRAY} requires a {@link Collection}, the values of this collection are all being stringed using {@link Object#toString()}.
 * For custom objects in a collection, prefer to use {@link Gson}.
 */
public class DataObject {

    private final DataObjectId id;
    private PermissionKey readPermission, writePermission;
    private Long removalTime;
    private DataObjectType type;
    private String value;

    public DataObject(DataObjectId id) {
        this.id = id;
    }

    /**
     * Special constructor to make copies privately.
     * @param id the id of the data object
     * @param readPermission the read permission
     * @param writePermission the write permission
     * @param removalTime the time of removal: 0 = upon controller stop, -1 = never
     * @param type the type of the value
     * @param value the value
     */
    private DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, Long removalTime, DataObjectType type, String value) {
        this.id = id;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.removalTime = removalTime;
        this.type = type;
        this.value = value;
    }

    /**
     * Creates a new data object.
     * @param id the id of the data object
     * @param readPermission the read permission
     * @param writePermission the write permission
     * @param removalTime the time of removal: 0 = upon controller stop, -1 = never
     * @param type the type of the value
     * @param value the value
     */
    public DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, long removalTime, DataObjectType type, Object value) {
        this.id = id;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.removalTime = removalTime;
        this.type = type;
        this.value = value.toString();
    }

    public DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, DataObjectType type, Object value) {
        this(id, readPermission, writePermission, -1, type, value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, Long removalTime, DataObjectType type, Object value) {
        this(id, permission, permission, removalTime, type, value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, DataObjectType type, Object value) {
        this(id, permission, -1L, type, value);
    }

    public DataObject(DataObjectId id, Long removalTime, DataObjectType type, Object value) {
        this(id, PermissionKey.EVERYONE, removalTime, type, value);
    }

    public DataObject(DataObjectId id, DataObjectType type, Object value) {
        this(id, PermissionKey.EVERYONE, -1L, type, value);
    }

    /**
     * Creates a new data object by inferring the data type.
     * Items of collections are stringed using {@link Object#toString()}.
     * Unknown object types use {@link DataObjectType#BLOB}.
     * @param id the id of the data object
     * @param readPermission the read permission
     * @param writePermission the write permission
     * @param removalTime the time of removal: 0 = upon controller stop, -1 = never
     * @param value the value
     */
    public DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, long removalTime, Object value) {
        this.id = id;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.removalTime = removalTime;
        if (value instanceof String cast) {
            this.type = DataObjectType.STRING;
            this.value = cast;
        } else if (value instanceof Integer cast) {
            this.type = DataObjectType.INTEGER;
            this.value = cast.toString();
        } else if (value instanceof Long cast) {
            this.type = DataObjectType.LONG;
            this.value = cast.toString();
        } else if (value instanceof Float cast) {
            this.type = DataObjectType.FLOAT;
            this.value = cast.toString();
        } else if (value instanceof Double cast) {
            this.type = DataObjectType.DOUBLE;
            this.value = cast.toString();
        } else if (value instanceof Boolean cast) {
            this.type = DataObjectType.BOOLEAN;
            this.value = cast.toString();
        } else if (value instanceof Collection<?> cast) {
            this.type = DataObjectType.ARRAY;
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            cast.forEach(item -> joiner.add(item.toString()));
            this.value = joiner.toString();
        } else {
            this.type = DataObjectType.BLOB;
            this.value = value.toString();
        }
    }

    public DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, Object value) {
        this(id, readPermission, writePermission, -1, value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, long removalTime, Object value) {
        this(id, permission, permission, removalTime, value);
    }

    public DataObject(DataObjectId id, long removalTime, Object value) {
        this(id, PermissionKey.EVERYONE, removalTime, value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, Object value) {
        this(id, permission, -1, value);
    }

    public DataObject(DataObjectId id, Object value) {
        this(id, PermissionKey.EVERYONE, -1, value);
    }

    /**
     * Creates a new data object by inferring the data type using the provided {@link Gson}.
     * Items of collections are stringed using by using the {@link Gson} object.
     * Unknown object types use {@link DataObjectType#JSON}.
     * @param id the id of the data object
     * @param readPermission the read permission
     * @param writePermission the write permission
     * @param removalTime the time of removal: 0 = upon controller stop, -1 = never
     * @param gson the gson
     * @param value the value
     */
    public DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, long removalTime, Gson gson, Object value) {
        this.id = id;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.removalTime = removalTime;
        if (value instanceof String cast) {
            this.type = DataObjectType.STRING;
            this.value = cast;
        } else if (value instanceof Integer cast) {
            this.type = DataObjectType.INTEGER;
            this.value = cast.toString();
        } else if (value instanceof Long cast) {
            this.type = DataObjectType.LONG;
            this.value = cast.toString();
        } else if (value instanceof Float cast) {
            this.type = DataObjectType.FLOAT;
            this.value = cast.toString();
        } else if (value instanceof Double cast) {
            this.type = DataObjectType.DOUBLE;
            this.value = cast.toString();
        } else if (value instanceof Boolean cast) {
            this.type = DataObjectType.BOOLEAN;
            this.value = cast.toString();
        } else if (value instanceof Collection<?> cast) {
            this.type = DataObjectType.ARRAY;
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            cast.forEach(item -> joiner.add(gson.toJson(item)));
            this.value = joiner.toString();
        } else {
            this.type = DataObjectType.JSON;
            this.value = gson.toJson(value);
        }
    }

    public DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, Gson gson, Object value) {
        this(id, readPermission, writePermission, -1, gson, value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, long removalTime, Gson gson, Object value) {
        this(id, permission, permission, removalTime, gson, value);
    }

    public DataObject(DataObjectId id, long removalTime, Gson gson, Object value) {
        this(id, PermissionKey.EVERYONE, removalTime, gson, value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, Gson gson, Object value) {
        this(id, permission, -1, gson, value);
    }

    public DataObject(DataObjectId id, Gson gson, Object value) {
        this(id, PermissionKey.EVERYONE, -1, gson, value);
    }

    /**
     * Returns whether all values are set.
     * This is useful as writing data objects need all information when writing.
     * @return true if all values are set.
     */
    public boolean isWriteable() {
        return readPermission != null && writePermission != null && removalTime != null && type != null && value != null;
    }

    /**
     * Creates a new data object with the information of this object that it has access to.
     * @return the new data object
     */
    public DataObject shallowCopy(boolean hasReadPermission, boolean hasWritePermission) {
        if(!hasReadPermission && !hasWritePermission) {
            // Nothing, only ID
            return new DataObject(id, null, null, null, null, null);
        }
        if(hasReadPermission && !hasWritePermission) {
            // Everything besides write permission
            return new DataObject(id, readPermission, null, removalTime, type, value);
        }
        // Always write permission
        if(!hasReadPermission) {
            // Only ID and write permission
            return new DataObject(id, null, writePermission, null, null, null);
        }
        // Got everything, so this object
        return this;
    }

    public DataObjectId getId() {
        return id;
    }

    public Optional<PermissionKey> getReadPermission() {
        return Optional.ofNullable(readPermission);
    }

    /**
     * Returns whether the given permission key has access to read the data object.
     * @param permissionKey the permission key
     * @param defaultValue the default value for when there is no permission stored in the object.
     * @return true if it has access, false otherwise; or the default value
     */
    public boolean hasReadAccess(PermissionKey permissionKey, boolean defaultValue) {
        return getReadPermission().map(key -> key.checkAccess(permissionKey)).orElse(defaultValue);
    }

    public Optional<PermissionKey> getWritePermission() {
        return Optional.ofNullable(writePermission);
    }

    /**
     * Returns whether the given permission key has access to write the data object.
     * @param permissionKey the permission key
     * @param defaultValue the default value for when there is no permission stored in the object.
     * @return true if it has access, false otherwise; or the default value
     */
    public boolean hasWriteAccess(PermissionKey permissionKey, boolean defaultValue) {
        return getWritePermission().map(key -> key.checkAccess(permissionKey)).orElse(defaultValue);
    }

    public Optional<Long> getRemovalTime() {
        return Optional.ofNullable(removalTime);
    }

    public Optional<DataObjectType> getType() {
        return Optional.ofNullable(type);
    }

    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

}
