package com.lahuca.lane.data.selector;

import com.lahuca.lane.data.DataObjectType;

/**
 * Determines the data order of the data selector.
 * By default, the path is null (meaning the value itself) and the cast type is DOUBLE.
 * @param path The JSONPath or null for the value itself.
 * @param cast The type to cast the path value to.
 * @param type The type of the order.
 */
public record DataOrder(String path, DataObjectType cast, DataOrderType type) {

    public DataOrder(String path, DataObjectType cast) {
        this(path, cast, DataOrderType.DESCENDING);
    }

    public DataOrder(String path) {
        this(path, DataObjectType.DOUBLE, DataOrderType.DESCENDING);
    }

    public DataOrder(DataObjectType cast) {
        this(null, cast);
    }

    public DataOrder() {
        this(DataObjectType.DOUBLE);
    }

}
