package com.lahuca.lane.data;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.*;

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
 * Prefer to always use the constructor with {@link Gson} when using type {@link DataObjectType#ARRAY}, as otherwise JSON is processed manually.
 * For custom objects in a collection, prefer to use {@link Gson}.
 * The same holds for using setValue.
 * @see #setValue(Gson, Object)
 */
public class DataObject {

    private final DataObjectId id;
    private PermissionKey readPermission, writePermission;
    private Long lastUpdated;
    private Long removalTime;
    private Integer version;
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
     * @param version the version of the data
     * @param type the type of the value
     * @param value the value
     */
    private DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, Long lastUpdated, Long removalTime, Integer version, DataObjectType type, String value) {
        this.id = id;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.lastUpdated = lastUpdated;
        this.removalTime = removalTime;
        this.version = version;
        this.type = type;
        this.value = value;
    }

    /**
     * Creates a new data object.
     * @param id the id of the data object
     * @param readPermission the read permission
     * @param writePermission the write permission
     * @param removalTime the time of removal: 0 = upon controller stop, -1 = never
     * @param version the version of the data
     * @param type the type of the value
     * @param value the value
     */
    public DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, long removalTime, int version, DataObjectType type, Object value) {
        this.id = id;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.removalTime = removalTime;
        this.version = version;
        this.type = type;
        this.value = value.toString();
    }

    public DataObject(DataObjectId id, PermissionKey permission, Long removalTime, int version, DataObjectType type, Object value) {
        this(id, permission, permission, removalTime, version, type, value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, DataObjectType type, Object value) {
        this(id, permission, -1L, 0, type, value);
    }

    public DataObject(DataObjectId id, DataObjectType type, Object value) {
        this(id, PermissionKey.EVERYONE, type, value);
    }

    /**
     * Creates a new data object by inferring the data type.
     * Items of collections are stringed using {@link Object#toString()}.
     * Unknown object types use {@link DataObjectType#BLOB}.
     * @param id the id of the data object
     * @param readPermission the read permission
     * @param writePermission the write permission
     * @param removalTime the time of removal: 0 = upon controller stop, -1 = never
     * @param version the version of the data
     * @param value the value
     * @see #setValue(Object)
     */
    public DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, long removalTime, int version, Object value) {
        this.id = id;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.removalTime = removalTime;
        this.version = version;
        setValue(value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, long removalTime, int version, Object value) {
        this(id, permission, permission, removalTime, version, value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, Object value) {
        this(id, permission, -1, 0, value);
    }

    public DataObject(DataObjectId id, Object value) {
        this(id, PermissionKey.EVERYONE, value);
    }

    /**
     * Creates a new data object by inferring the data type using the provided {@link Gson}.
     * Items of collections are stringed using by using the {@link Gson} object.
     * Unknown object types use {@link DataObjectType#JSON}.
     * @param id the id of the data object
     * @param readPermission the read permission
     * @param writePermission the write permission
     * @param removalTime the time of removal: 0 = upon controller stop, -1 = never
     * @param version the version of the data
     * @param gson the gson
     * @param value the value
     */
    public DataObject(DataObjectId id, PermissionKey readPermission, PermissionKey writePermission, long removalTime, int version, Gson gson, Object value) {
        this.id = id;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.removalTime = removalTime;
        this.version = version;
        setValue(gson, value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, long removalTime, int version, Gson gson, Object value) {
        this(id, permission, permission, removalTime, version, gson, value);
    }

    public DataObject(DataObjectId id, PermissionKey permission, Gson gson, Object value) {
        this(id, permission, -1, 0, gson, value);
    }

    public DataObject(DataObjectId id, Gson gson, Object value) {
        this(id, PermissionKey.EVERYONE, gson, value);
    }

    /**
     * Returns whether all values are set.
     * This is useful as writing data objects need all information when writing.
     * @return true if all values are set.
     */
    public boolean isWriteable() {
        return readPermission != null && writePermission != null && removalTime != null && version != null && type != null && value != null
                && readPermission.isFormattedCorrectly() && writePermission.isFormattedCorrectly();
    }

    /**
     * Creates a new data object with the information of this object that it has access to.
     * A new ID can be provided to create a data object with a new ID.
     * @param newId the new ID, null if it should remain the same
     * @param hasReadPermission whether the copy should be copied as if it has reading permissions
     * @param hasWritePermission whether the copy should be copied as if it has writing permissions
     * @return the new data object
     */
    public DataObject shallowCopy(DataObjectId newId, boolean hasReadPermission, boolean hasWritePermission) {
        DataObjectId id = newId != null ? newId : this.id;
        if(!hasReadPermission && !hasWritePermission) {
            // Nothing, only ID
            return new DataObject(id, null, null, null, null, null, null, null);
        }
        if(hasReadPermission && !hasWritePermission) {
            // Everything besides write permission
            return new DataObject(id, readPermission, null, lastUpdated, removalTime, version, type, value);
        }
        // Always write permission
        if(!hasReadPermission) {
            // Only ID and write permission
            return new DataObject(id, null, writePermission, null, null, null, null, null);
        }
        // Got everything, if ID is the same, return this
        if(newId == null) return this;
        // Return all data, but with different ID
        return new DataObject(id, readPermission, writePermission, lastUpdated, removalTime, version, type, value);
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

    public Optional<Long> getLastUpdated() {
        return Optional.ofNullable(lastUpdated);
    }

    public Optional<Long> getRemovalTime() {
        return Optional.ofNullable(removalTime);
    }

    /**
     * Returns whether the current data object should be removed.
     * Either because it should be removed when the controller has stopped;
     * or when the time has passed.
     * @param startTime the time the controller has started
     * @return true if it should be removed, false otherwise.
     */
    public boolean shouldRemove(long startTime) {
        if(removalTime == null) return false;
        // Check if time passed
        if(removalTime > 0 && System.currentTimeMillis() >= removalTime) return true;
        // Check if controller has restarted
        return removalTime == 0 && lastUpdated != null && lastUpdated < startTime;
    }

    public Optional<Integer> getVersion() {
        return Optional.ofNullable(version);
    }

    public Optional<DataObjectType> getType() {
        return Optional.ofNullable(type);
    }

    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    /**
     * Gets the value as an integer.
     * When the value is present and can be parsed, this optional will not be empty.
     * @return the optional of the value
     */
    public Optional<Integer> getValueAsInteger() {
        try {
            return getValue().map(Integer::valueOf);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the value as a long.
     * When the value is present and can be parsed, this optional will not be empty.
     * @return the optional of the value
     */
    public Optional<Long> getValueAsLong() {
        try {
            return getValue().map(Long::valueOf);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the value as a float.
     * When the value is present and can be parsed, this optional will not be empty.
     * @return the optional of the value
     */
    public Optional<Float> getValueAsFloat() {
        try {
            return getValue().map(Float::valueOf);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the value as a double.
     * When the value is present and can be parsed, this optional will not be empty.
     * @return the optional of the value
     */
    public Optional<Double> getValueAsDouble() {
        try {
            return getValue().map(Double::valueOf);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the value as a boolean.
     * When the value is present, this optional will not be empty.
     * Only when the value is equal to "true", the boolean will be true.
     * @return the optional of the value
     */
    public Optional<Boolean> getValueAsBoolean() {
        return getValue().map(Boolean::valueOf);
    }

    /**
     * Gets the value as a certain object by using the provided Gson and class.
     * When the value is present and the value represents the given class, this optional will not be empty.
     * @return the optional of the value
     */
    public <T> Optional<T> getValueAsJson(Gson gson, Class<T> type) {
        try {
            return getValue().map(value -> gson.fromJson(value, type));
        } catch (JsonSyntaxException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the value as a certain object by using the provided Gson and type.
     * When the value is present and the value represents the given type, this optional will not be empty.
     * @return the optional of the value
     */
    public <T> Optional<T> getValueAsJson(Gson gson, TypeToken<T> type) {
        try {
            return getValue().map(value -> gson.fromJson(value, type));
        } catch (JsonSyntaxException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the value as a certain object by using the provided Gson and class.
     * When the value is present and the value represents the given class, this optional will not be empty.
     * @return the optional of the value
     * @see #getValueAsJson(Gson, Class)
     */
    public <T> Optional<T> getValue(Gson gson, Class<T> type) {
        return getValueAsJson(gson, type);
    }

    /**
     * Gets the value as a certain object by using the provided Gson and type.
     * When the value is present and the value represents the given type, this optional will not be empty.
     * @return the optional of the value
     * @see #getValueAsJson(Gson, Class)
     */
    public <T> Optional<T> getValue(Gson gson, TypeToken<T> type) {
        return getValueAsJson(gson, type);
    }

    /**
     * Gets the value as a string list.
     * When the value is present and the value represents a string list, this optional will not be empty.
     * @return the optional of the value
     */
    public Optional<List<String>> getValueAsStringArray(Gson gson) {
        return getValueAsJson(gson, new TypeToken<>(){});
    }

    /**
     * Gets the value as an integer list.
     * When the value is present and the value represents an integer list, this optional will not be empty.
     * @return the optional of the value
     */
    public Optional<List<Integer>> getValueAsIntegerArray(Gson gson) {
        return getValueAsJson(gson, new TypeToken<>(){});
    }

    /**
     * Gets the value as a long list.
     * When the value is present and the value represents a long list, this optional will not be empty.
     * @return the optional of the value
     */
    public Optional<List<Long>> getValueAsLongArray(Gson gson) {
        return getValueAsJson(gson, new TypeToken<>(){});
    }

    /**
     * Gets the value as a float list.
     * When the value is present and the value represents a float list, this optional will not be empty.
     * @return the optional of the value
     */
    public Optional<List<Float>> getValueAsFloatArray(Gson gson) {
        return getValueAsJson(gson, new TypeToken<>(){});
    }

    /**
     * Gets the value as a double list.
     * When the value is present and the value represents a double list, this optional will not be empty.
     * @return the optional of the value
     */
    public Optional<List<Double>> getValueAsDoubleArray(Gson gson) {
        return getValueAsJson(gson, new TypeToken<>(){});
    }

    /**
     * Gets the value as a boolean list.
     * When the value is present and the value represents a boolean list, this optional will not be empty.
     * @return the optional of the value
     */
    public Optional<List<Boolean>> getValueAsBooleanArray(Gson gson) {
        return getValueAsJson(gson, new TypeToken<>(){});
    }

    public void setReadPermission(PermissionKey readPermission) {
        this.readPermission = readPermission;
    }

    public void setWritePermission(PermissionKey writePermission) {
        this.writePermission = writePermission;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setRemovalTime(Long removalTime) {
        this.removalTime = removalTime;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Sets the value with the given type and object.
     * The object is saved by using {@link Object#toString()}.
     * This does not update the data object in the data storage system.
     * @param type the type of the value
     * @param value the object
     */
    public void setValue(DataObjectType type, Object value) {
        this.type = type;
        this.value = value.toString();
    }

    /**
     * Sets the value with the given type and object.
     * Tries to infer the type by using instanceof.
     * {@link Collection} is mapped to {@link DataObjectType#ARRAY} by using manual JSON parsing.
     * This manual JSON parsing always maps to a string collection.
     * Unknown object types use {@link DataObjectType#BLOB}.
     * Prefer to use {@link #setValue(Gson, Object)} when using {@link Collection} objects.
     * This does not update the data object in the data storage system.
     * @param value the object
     */
    public void setValue(Object value) {
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
            cast.forEach(item -> joiner.add('"' + item.toString().replace("\"", "\\\"") + '"'));
            this.value = cast.toString();
        } else {
            this.type = DataObjectType.BLOB;
            this.value = value.toString();
        }
    }

    /**
     * Sets the value with the given type and object.
     * Tries to infer the type by using instanceof, otherwise the provided {@link Gson} is used.
     * Unknown object types use {@link DataObjectType#JSON}.
     * This does not update the data object in the data storage system.
     * @param gson the gson
     * @param value the object
     */
    public void setValue(Gson gson, Object value) {
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
            this.value = gson.toJson(cast);
        } else {
            this.type = DataObjectType.JSON;
            this.value = gson.toJson(value);
        }
    }

}
