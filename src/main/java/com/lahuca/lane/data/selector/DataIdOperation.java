package com.lahuca.lane.data.selector;

/**
 * These values determine what needs to happen to the relational ID/ID of said selector.
 */
public enum DataIdOperation {

    /**
     * The value has to match exactly with the provided value.
     */
    EXACT,
    /**
     * The value has to start with the provided value.
     */
    PREFIX,
    /**
     * The value can be any value.
     */
    ANY

}
