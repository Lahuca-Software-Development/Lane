package com.lahuca.lane.data.selector;

import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.lahuca.lane.utilities.GsonUtilities;

/**
 * Determines the interface for our data filters.
 */
public sealed interface DataFilter permits DataFilter.Selection, DataFilter.DataFilterLogical, DataFilter.DataFilterNumerical {

    /**
     * Tests the object for the given filter.
     * @param object the object to test
     * @return true if the object passes the filter, false otherwise
     */
    boolean filter(String selection, Object object);

    RuntimeTypeAdapterFactory<DataFilter> FACTORY = GsonUtilities.getSealedRuntimeTypeAdapterFactory(DataFilter.class);

    /**
     * Sets the selection of the default path to the given path for the filter.
     * When the path at some child filter is set to null, this selected path will be used instead of the value itself.
     * @param path the path to select
     * @param filter the filter to set the default value of
     */
    record Selection(String path, DataFilter filter) implements DataFilter {

        @Override
        public boolean filter(String selection, Object object) {
            throw new UnsupportedOperationException("JSONPath is not supported for it on the file manager."); // TODO Support JSONPath for file manager
        }

    }

    /**
     * Determines the interface for our logical data filters.
     * Logical data filters combine data filters.
     */
    sealed interface DataFilterLogical extends DataFilter permits And, Or, Not {}

    /**
     * Combines multiple data filters in an AND fashion.
     * @param filters the filters to combine
     */
    record And(DataFilter... filters) implements DataFilterLogical {

        @Override
        public boolean filter(String selection, Object object) {
            for (DataFilter filter : filters) {
                if (!filter.filter(selection, object)) return false;
            }
            return true;
        }

    }

    /**
     * Combines multiple data filters in an OR fashion.
     * @param filters the filters to combine
     */
    record Or(DataFilter... filters) implements DataFilterLogical {

        @Override
        public boolean filter(String selection, Object object) {
            for (DataFilter filter : filters) {
                if (filter.filter(selection, object)) return true;
            }
            return false;
        }

    }

    /**
     * Negates a data filter.
     * @param filter the filter to negate
     */
    record Not(DataFilter filter) implements DataFilterLogical {

        @Override
        public boolean filter(String selection, Object object) {
            return !filter.filter(selection, object);
        }

    }

    sealed interface DataFilterNumerical extends DataFilter permits Equals, LowerThan, LowerThanEquals, GreaterThan, GreaterThanEquals, Between {

        String path();

    }

    /**
     * Filters on the path: the value at the path must be the provided value.
     * @param path the JSON path to filter on, null for the value itself or the selection
     * @param value the value to compare with
     */
    record Equals(String path, Number value) implements DataFilterNumerical {

        public Equals(Number value) {
            this(null, value);
        }

        public Equals(String path, boolean value) {
            this(path, value ? 1 : 0);
        }

        public Equals(boolean value) {
            this(null, value);
        }

        @Override
        public boolean filter(String selection, Object object) {
            // TODO Use selection!
            return object instanceof Number number && number.doubleValue() == value.doubleValue();
        }

    }

    /**
     * Filters on the path: the value at the path must not be the provided value.
     * Shorthand for: Not(Equals(path, value))
     * @param path the JSON path to filter on, null for the value itself or the selection
     * @param value the value to compare with
     * @return the negated filter
     */
    static Not NotEquals(String path, Number value) {
        return new Not(new Equals(path, value));
    }

    static Not NotEquals(Number value) {
        return NotEquals(null, value);
    }

    static Not NotEquals(String path, boolean value) {
        return NotEquals(path, value ? 1 : 0);
    }

    static Not NotEquals(boolean value) {
        return NotEquals(null, value);
    }

    /**
     * Filters on the path: the value at the path must be equal to 1 (true).
     * Shorthand for: Equals(path, 1)
     * @param path the JSON path to filter on, null for the value itself or the selection
     * @return the negated filter
     */
    static Equals IsTrue(String path) {
        return new Equals(path, 1);
    }

    static Equals IsTrue() {
        return IsTrue(null);
    }

    /**
     * Filters on the path: the value at the path must be equal to 0 (false).
     * Shorthand for: Equals(path, 0)
     * @param path the JSON path to filter on, null for the value itself or the selection
     * @return the negated filter
     */
    static Equals IsFalse(String path) {
        return new Equals(path, 0);
    }

    static Equals IsFalse() {
        return IsFalse(null);
    }

    /**
     * Filters on the path: the value at the path must be lower than the provided value.
     * @param path the JSON path to filter on, null for the value itself or the selection
     * @param value the value to compare with
     */
    record LowerThan(String path, Number value) implements DataFilterNumerical {

        public LowerThan(Number value) {
            this(null, value);
        }

        @Override
        public boolean filter(String selection, Object object) {
            return object instanceof Number number && number.doubleValue() < value.doubleValue();
        }

    }

    /**
     * Filters on the path: the value at the path must be lower than or equal the provided value.
     * @param path the JSON path to filter on, null for the value itself or the selection
     * @param value the value to compare with
     */
    record LowerThanEquals(String path, Number value) implements DataFilterNumerical {

        public LowerThanEquals(Number value) {
            this(null, value);
        }

        @Override
        public boolean filter(String selection, Object object) {
            return object instanceof Number number && number.doubleValue() <= value.doubleValue();
        }

    }

    /**
     * Filters on the path: the value at the path must be greater than the provided value.
     * @param path the JSON path to filter on, null for the value itself or the selection
     * @param value the value to compare with
     */
    record GreaterThan(String path, Number value) implements DataFilterNumerical {

        public GreaterThan(Number value) {
            this(null, value);
        }

        @Override
        public boolean filter(String selection, Object object) {
            return object instanceof Number number && number.doubleValue() > value.doubleValue();
        }

    }

    /**
     * Filters on the path: the value at the path must be greater than or equal the provided value.
     * @param path the JSON path to filter on, null for the value itself or the selection
     * @param value the value to compare with
     */
    record GreaterThanEquals(String path, Number value) implements DataFilterNumerical {

        public GreaterThanEquals(Number value) {
            this(null, value);
        }

        @Override
        public boolean filter(String selection, Object object) {
            return object instanceof Number number && number.doubleValue() >= value.doubleValue();
        }

    }

    /**
     * Filters on the path: the value at the path must be greater than the lower bound and smaller than the upper bound.
     * @param path the JSON path to filter on, null for the value itself or the selection
     * @param lower the lower bound
     * @param upper the upper bound
     */
    record Between(String path, Number lower, Number upper) implements DataFilterNumerical {

        public Between(Number lower, Number upper) {
            this(null, lower, upper);
        }

        @Override
        public boolean filter(String selection, Object object) {
            return object instanceof Number number && lower.doubleValue() <= number.doubleValue() && number.doubleValue() <= upper.doubleValue();
        }

    }

}
