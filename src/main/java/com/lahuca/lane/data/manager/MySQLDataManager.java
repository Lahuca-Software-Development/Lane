package com.lahuca.lane.data.manager;

import com.google.gson.Gson;
import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.DataObjectType;
import com.lahuca.lane.data.PermissionKey;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Data Manager that uses MySQL databases:
 * <p>
 * Two options have been investigated:
 * 1. For every DataObjectType a new column.
 * 2. Store in a single JSON/TEXT.
 * Option 1: This will add a lot of NULL values, loosing storage space.
 * Option 2: This will not allow for more complex computations. At least they take more performance.
 * The second option has been chosen.
 * <p>
 * The singular table has the following columns:
 * <ul>
 *     <li>ID (id)</li>
 *     <li>Read permission (read_permission)</li>
 *     <li>Write permission (write_permission)</li>
 *     <li>Last updated (last_updated)</li>
 *     <li>Removal time (removal_time)</li>
 *     <li>Version (version)</li>
 *     <li>Type (type)</li>
 *     <li>Value (value)</li>
 * </ul>
 * The respective relational tables also have an added relational ID (relational_id).
 */
public class MySQLDataManager implements DataManager {

    private final Gson gson;
    private final DataSource dataSource;
    private final String prefix;
    private final long startTime = System.currentTimeMillis();
    private final HashSet<DataObjectId> removeOnStop = new HashSet<>(); // TODO Maybe too much RAM usage?

    public MySQLDataManager(Gson gson, DataSource dataSource, String prefix) {
        this.gson = gson;
        this.dataSource = dataSource;
        this.prefix = prefix;
    }

    @Override
    public void shutdown() {
        removeOnStop.forEach(id -> removeDataObject(PermissionKey.CONTROLLER, id));
        if(dataSource instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Could not close, but we are in a shutdown?
                e.printStackTrace(); // TODO Probably log?
            }
        }
    }

    private String getTableName(DataObjectId id) {
        if(id.id().isEmpty() || id.id().length() > 128) return null;
        if(id.isRelational() && (id.relationalId().type().isEmpty() || id.relationalId().type().length() > 64
                || id.relationalId().id().isEmpty() || !id.relationalId().type().matches("[a-zA-Z]+"))) return null;
        return prefix + (id.isRelational() ? "_relational_" + id.relationalId().type() : "_singular");
    }

    private static <T> CompletableFuture<Optional<T>> empty() {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Optional<DataObject>> readDataObject(PermissionKey permissionKey, DataObjectId id) {
        String tableName = getTableName(id);
        if(tableName == null) return empty();
        try(Connection connection = dataSource.getConnection()) {
            // Build select query
            PreparedStatement statement;
            if(id.isRelational()) {
                statement = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE relational_id = ? AND id = ?");
                statement.setString(1, id.relationalId().id());
                statement.setString(2, id.id());
            } else {
                statement = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE id = ?");
                statement.setString(1, id.id());
            }
            // Get result of query
            try(ResultSet resultSet = statement.executeQuery()) {
                if(resultSet.next()) {
                    String readPermission = resultSet.getString("read_permission");
                    String writePermission = resultSet.getString("write_permission");
                    Timestamp lastUpdated = resultSet.getTimestamp("last_updated");
                    long removalTime = resultSet.getLong("removal_time");
                    int version = resultSet.getInt("version");
                    String typeString = resultSet.getString("type");
                    if(readPermission == null || writePermission == null || typeString == null) {
                        return empty();
                    }
                    try {
                        DataObjectType type = DataObjectType.valueOf(typeString);
                        String value = resultSet.getString("value");
                        if(value == null) return empty();
                        if(type == DataObjectType.STRING) {
                            value = gson.fromJson(value, String.class);
                        }
                        // Do permission logic and return correct value.
                        DataObject object = new DataObject(id, PermissionKey.fromString(readPermission),
                                PermissionKey.fromString(writePermission), removalTime, version, type, value);
                        object.setLastUpdated(lastUpdated == null ? null : lastUpdated.getTime());
                        if(object.shouldRemove(startTime)) {
                            return removeDataObject(PermissionKey.CONTROLLER, id).thenApply(status -> Optional.empty());
                        }
                        boolean readAccess = object.hasReadAccess(permissionKey, true);
                        boolean writeAccess = object.hasWriteAccess(permissionKey, false);
                        object = object.shallowCopy(readAccess, writeAccess);
                        return CompletableFuture.completedFuture(Optional.of(object));
                    } catch (IllegalArgumentException e) {
                        return empty();
                    }
                }
                return empty();
            }
        } catch (SQLException e) {
            return empty();
        }
    }

    /**
     * This method is used to handle the writeDataObject.
     * The additional boolean value determines whether it should be tried to create the table whenever it does not exist yet.
     * This makes sure that the method is only called once recursively.
     * Be aware that data objects with type {@link DataObjectType#BLOB} might be read incorrectly.
     * @param permissionKey the permission key to use while writing
     * @param object the object to write
     * @param madeTable whether the table has been made
     * @return the status
     * @see #writeDataObject(PermissionKey, DataObject)
     */
    private CompletableFuture<Boolean> writeDataObject(PermissionKey permissionKey, DataObject object, boolean madeTable) {
        if(!object.isWriteable()) return CompletableFuture.completedFuture(null);
        if(!object.hasWriteAccess(permissionKey, false)) return CompletableFuture.completedFuture(false);
        DataObjectId id = object.getId();
        String tableName = getTableName(id);
        if(tableName == null) return CompletableFuture.completedFuture(null);
        try(Connection connection = dataSource.getConnection()) {
            // First fetch the permission if it already exists. Make sure to lock it.
            connection.setAutoCommit(false);
            PreparedStatement select;
            if(id.isRelational()) {
                select = connection.prepareStatement("SELECT write_permission FROM " + tableName + " WHERE relational_id = ? AND id = ? FOR UPDATE");
                select.setString(1, id.relationalId().id());
                select.setString(2, id.id());
            } else {
                select = connection.prepareStatement("SELECT write_permission FROM " + tableName + " WHERE id = ? FOR UPDATE");
                select.setString(1, id.id());
            }
            boolean alreadyPresent = false;
            try(ResultSet resultSet = select.executeQuery()) {
                if(resultSet.next()) {
                    alreadyPresent = true;
                    String writePermissionString = resultSet.getString("write_permission");
                    // Odd, we found a match, but we did not get a permission.
                    if(writePermissionString == null) {
                        connection.setAutoCommit(true);
                        return CompletableFuture.completedFuture(null);
                    }
                    PermissionKey writePermission = PermissionKey.fromString(writePermissionString);
                    if(!writePermission.checkAccess(permissionKey)) {
                        // No permission
                        connection.setAutoCommit(true);
                        return CompletableFuture.completedFuture(false);
                    }
                }
            }
            // We can insert or update depending on if it is present already
            PreparedStatement update;
            if(alreadyPresent) {
                if(id.isRelational()) {
                    update = connection.prepareStatement("UPDATE " + tableName + " SET read_permission = ?, write_permission = ?, last_updated = ?, removal_time = ?, version = ?, `type` = ?, `value` = ? WHERE relational_id = ? AND id = ?");
                    update.setString(1, object.getReadPermission().orElse(PermissionKey.EVERYONE).toString());
                    update.setString(2, object.getWritePermission().orElse(PermissionKey.EVERYONE).toString());
                    update.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    update.setLong(4, object.getRemovalTime().orElse(-1L));
                    update.setInt(5, object.getVersion().orElse(0));
                    update.setString(6, object.getType().orElse(DataObjectType.BLOB).toString());
                    String value = object.getValue().orElse(null);
                    DataObjectType type = object.getType().orElse(DataObjectType.BLOB);
                    if(value != null && (type == DataObjectType.STRING || type == DataObjectType.BLOB)) value = gson.toJson(value);
                    update.setString(7, value);
                    update.setString(8, id.relationalId().id());
                    update.setString(9, id.id());
                } else {
                    update = connection.prepareStatement("UPDATE " + tableName + " SET read_permission = ?, write_permission = ?, last_updated = ?, removal_time = ?, version = ?, `type` = ?, `value` = ? WHERE id = ?");
                    update.setString(1, object.getReadPermission().orElse(PermissionKey.EVERYONE).toString());
                    update.setString(2, object.getWritePermission().orElse(PermissionKey.EVERYONE).toString());
                    update.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    update.setLong(4, object.getRemovalTime().orElse(-1L));
                    update.setInt(5, object.getVersion().orElse(0));
                    update.setString(6, object.getType().orElse(DataObjectType.BLOB).toString());
                    String value = object.getValue().orElse(null);
                    DataObjectType type = object.getType().orElse(DataObjectType.BLOB);
                    if(value != null && (type == DataObjectType.STRING || type == DataObjectType.BLOB)) value = gson.toJson(value);
                    update.setString(7, value);
                    update.setString(8, id.id());
                }
            } else {
                if(id.isRelational()) {
                    update = connection.prepareStatement("INSERT INTO " + tableName + " (relational_id, id, read_permission, write_permission, last_updated, removal_time, version, `type`, `value`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    update.setString(1, id.relationalId().id());
                    update.setString(2, id.id());
                    update.setString(3, object.getReadPermission().orElse(PermissionKey.EVERYONE).toString());
                    update.setString(4, object.getWritePermission().orElse(PermissionKey.EVERYONE).toString());
                    update.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                    update.setLong(6, object.getRemovalTime().orElse(-1L));
                    update.setInt(7, object.getVersion().orElse(0));
                    update.setString(8, object.getType().orElse(DataObjectType.BLOB).toString());
                    String value = object.getValue().orElse(null);
                    DataObjectType type = object.getType().orElse(DataObjectType.BLOB);
                    if(value != null && (type == DataObjectType.STRING || type == DataObjectType.BLOB)) value = gson.toJson(value);
                    update.setString(9, value);
                } else {
                    update = connection.prepareStatement("INSERT INTO " + tableName + " (id, read_permission, write_permission, last_updated, removal_time, version, `type`, `value`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                    update.setString(1, id.id());
                    update.setString(2, object.getReadPermission().orElse(PermissionKey.EVERYONE).toString());
                    update.setString(3, object.getWritePermission().orElse(PermissionKey.EVERYONE).toString());
                    update.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    update.setLong(5, object.getRemovalTime().orElse(-1L));
                    update.setInt(6, object.getVersion().orElse(0));
                    update.setString(7, object.getType().orElse(DataObjectType.BLOB).toString());
                    String value = object.getValue().orElse(null);
                    DataObjectType type = object.getType().orElse(DataObjectType.BLOB);
                    if(value != null && (type == DataObjectType.STRING || type == DataObjectType.BLOB)) value = gson.toJson(value);
                    update.setString(8, value);
                }
            }
            update.executeUpdate();
            connection.setAutoCommit(true);
            object.getRemovalTime().ifPresent(removalTime -> {
                if(removalTime == 0) {
                    removeOnStop.add(object.getId());
                } else {
                    removeOnStop.remove(object.getId());
                }
            });
            return CompletableFuture.completedFuture(true);
        } catch (SQLException e) {
            if((e.getErrorCode() == 1051 || e.getErrorCode() == 1146) && !madeTable) {
                // Unknown table, create and retry!
                try(Connection connection = dataSource.getConnection()) {
                    connection.setAutoCommit(false);
                    PreparedStatement statement;
                    if(id.isRelational()) {
                        statement = connection.prepareStatement("CREATE TABLE " + tableName + """
                             (
                                relational_id VARCHAR(128) NOT NULL,
                                id VARCHAR(128) NOT NULL,
                                read_permission VARCHAR(39) NOT NULL,
                                write_permission VARCHAR(39) NOT NULL,
                                last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                removal_time BIGINT DEFAULT -1,
                                version INT NOT NULL DEFAULT 0,
                                `type` VARCHAR(32) NOT NULL,
                                `value` JSON NOT NULL,
                                PRIMARY KEY (relational_id, id)
                            );""");
                    } else {
                        statement = connection.prepareStatement("CREATE TABLE " + tableName + """
                             (
                                id VARCHAR(128) NOT NULL PRIMARY KEY,
                                read_permission VARCHAR(39) NOT NULL,
                                write_permission VARCHAR(39) NOT NULL,
                                last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                removal_time BIGINT DEFAULT -1,
                                version INT NOT NULL DEFAULT 0,
                                `type` VARCHAR(32) NOT NULL,
                                `value` JSON NOT NULL
                            );""");
                    }
                    statement.executeUpdate();
                    return writeDataObject(permissionKey, object, true);
                } catch (SQLException ex2) {
                    return CompletableFuture.completedFuture(null);
                }
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<Boolean> writeDataObject(PermissionKey permissionKey, DataObject object) {
        return writeDataObject(permissionKey, object, false);
    }

    @Override
    public CompletableFuture<Boolean> removeDataObject(PermissionKey permissionKey, DataObjectId id) {
        String tableName = getTableName(id);
        if(tableName == null) return CompletableFuture.completedFuture(null);
        try(Connection connection = dataSource.getConnection()) {
            // Build select query
            connection.setAutoCommit(false);
            PreparedStatement select;
            if(id.isRelational()) {
                select = connection.prepareStatement("SELECT write_permission FROM " + tableName + " WHERE relational_id = ? AND id = ? FOR UPDATE");
                select.setString(1, id.relationalId().id());
                select.setString(2, id.id());
            } else {
                select = connection.prepareStatement("SELECT write_permission FROM " + tableName + " WHERE id = ? FOR UPDATE");
                select.setString(1, id.id());
            }
            try(ResultSet resultSet = select.executeQuery()) {
                if(resultSet.next()) {
                    String writePermissionString = resultSet.getString("write_permission");
                    // Odd, we found a match, but we did not get a permission.
                    if(writePermissionString == null) {
                        connection.setAutoCommit(true);
                        return CompletableFuture.completedFuture(null);
                    }
                    PermissionKey writePermission = PermissionKey.fromString(writePermissionString);
                    if(!writePermission.checkAccess(permissionKey)) {
                        // No permission
                        connection.setAutoCommit(true);
                        return CompletableFuture.completedFuture(false);
                    }
                    // We can remove it!
                    PreparedStatement delete;
                    if(id.isRelational()) {
                        delete = connection.prepareStatement("DELETE FROM " + tableName + " WHERE relational_id = ? AND id = ?");
                        delete.setString(1, id.relationalId().id());
                        delete.setString(2, id.id());
                    } else {
                        delete = connection.prepareStatement("DELETE FROM " + tableName + " WHERE id = ?");
                        delete.setString(1, id.id());
                    }
                    delete.executeUpdate();
                    connection.setAutoCommit(true);
                    return CompletableFuture.completedFuture(true);
                } else {
                    connection.setAutoCommit(true);
                    return CompletableFuture.completedFuture(true);
                }
            }
        } catch (SQLException e) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
