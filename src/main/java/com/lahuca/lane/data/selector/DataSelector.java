package com.lahuca.lane.data.selector;

import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.RelationalId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * A builder class that resembles a selector for querying data in an object-oriented fashion.
 * The data selector is given the data object ID as a starting point.
 * If the relational ID's table is set, this is where the data is being queried, otherwise the singular table.
 * If the relational ID's ID or ID are set, the corresponding {@link DataIdOperation}s are used to figure out what to do: exact match, prefix match or no required match.
 * A data order can be given to sort the results.
 */
public class DataSelector {

    private DataObjectId id = new DataObjectId();
    private DataIdOperation relationalIdOperation = DataIdOperation.ANY;
    private DataIdOperation idOperation = DataIdOperation.ANY;

    private DataOrder[] order;
    private Long limit;
    private Long offset;

    public DataSelector(@NotNull DataObjectId id, @NotNull DataIdOperation relationalIdOperation, @NotNull DataIdOperation idOperation) {
        this.id = id;
        this.relationalIdOperation = relationalIdOperation;
        this.idOperation = idOperation;
    }

    public DataSelector(@NotNull DataObjectId id) {
        this.id = id;
    }

    public DataObjectId getId() {
        return id;
    }

    public DataIdOperation getIdOperation() {
        return idOperation;
    }

    public DataIdOperation getRelationalIdOperation() {
        return relationalIdOperation;
    }

    public DataOrder[] getOrder() {
        return order;
    }

    public DataSelector order(DataOrder... order) {
        this.order = order;
        return this;
    }

    public Long getLimit() {
        return limit;
    }

    public DataSelector limit(Long limit) {
        this.limit = limit;
        return this;
    }

    public Long getOffset() {
        return offset;
    }

    public DataSelector offset(Long offset) {
        this.offset = offset;
        return this;
    }

    public DataSelector limitOffset(Long limit, Long offset) {
        return limit(limit).offset(offset);
    }

}
