package com.lahuca.lane.data;

/**
 * Holder for the ID of a data object.
 * This can be used to point to data objects.
 * @param relationalId the relational part of the ID, can be null.
 * @param id the id.
 */
public record DataObjectId(RelationalId relationalId, String id) {

    public DataObjectId(String type, String relationalId, String id) {
        this(new RelationalId(type, relationalId), id);
    }

    public DataObjectId(String id) {
        this(null, id);
    }

    public boolean isRelational() {
        return relationalId != null && relationalId.type() != null && relationalId.id() != null;
    }

}
