package com.lahuca.lane.data.selector;

import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.RelationalId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable selector for querying data. Use {@link #builder()} to create instances.
 */
public record DataSelector(
        DataObjectId id,
        DataIdOperation relationalIdOperation,
        DataIdOperation idOperation,
        DataOrder[] order,
        Long limit,
        Long offset) {

    // -----------------------------------------------------------------
    // Default values when a component is not supplied by the builder
    // -----------------------------------------------------------------
    public DataSelector {
        // Apply defaults if null (the builder may leave fields null)
        id = Objects.requireNonNullElse(id, new DataObjectId());
        relationalIdOperation = Objects.requireNonNullElse(relationalIdOperation, DataIdOperation.ANY);
        idOperation = Objects.requireNonNullElse(idOperation, DataIdOperation.ANY);
    }

    public DataSelector order(DataOrder... order) {
        return new DataSelector(id, relationalIdOperation, idOperation, order, limit, offset);
    }

    public DataSelector limit(Long limit) {
        return new DataSelector(id, relationalIdOperation, idOperation, this.order, limit, offset);
    }

    public DataSelector offset(Long offset) {
        return new DataSelector(id, relationalIdOperation, idOperation, this.order, limit, offset);
    }

    public DataSelector limitOffset(Long limit, Long offset) {
        return limit(limit).offset(offset);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DataObjectId id;
        private DataIdOperation relationalIdOperation;
        private DataIdOperation idOperation;
        private DataOrder[] order;
        private Long limit;
        private Long offset;

        public Builder id(@NotNull DataObjectId id) {
            this.id = id;
            return this;
        }

        public Builder relationalIdOperation(@NotNull DataIdOperation relationalIdOperation) {
            this.relationalIdOperation = relationalIdOperation;
            return this;
        }

        public Builder idOperation(@NotNull DataIdOperation idOperation) {
            this.idOperation = idOperation;
            return this;
        }

        public Builder order(DataOrder... order) {
            this.order = order;
            return this;
        }

        public Builder limit(Long limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(Long offset) {
            this.offset = offset;
            return this;
        }

        public Builder limitOffset(Long limit, Long offset) {
            this.limit = limit;
            this.offset = offset;
            return this;
        }

        public DataSelector build() {
            return new DataSelector(
                    Objects.requireNonNullElse(this.id, new DataObjectId()),
                    Objects.requireNonNullElse(this.relationalIdOperation, DataIdOperation.ANY),
                    Objects.requireNonNullElse(this.idOperation, DataIdOperation.ANY),
                    this.order,
                    this.limit,
                    this.offset
            );
        }
    }
}