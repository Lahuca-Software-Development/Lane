package com.lahuca.lane.data;

/**
 * Determines the type of the value.
 * These are useful for the way the types are to be saved within the storage system.
 * These also help in the way they are cast.
 */
public enum DataObjectType {

    STRING, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN,
    DATE, TIME, TIMESTAMP,
    /**
     * The BLOB value is to be used whenever the data has no specific structure.
     */
    BLOB,
    /**
     * The JSON value is to be used whenever the data is formatted using JSON.
     */
    JSON,
    /**
     * The ARRAY value is to be used whenever the data is of type {@link java.util.Collection}.
     */
    ARRAY

}
