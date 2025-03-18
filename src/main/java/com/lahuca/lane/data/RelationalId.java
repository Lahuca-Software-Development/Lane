package com.lahuca.lane.data;

/**
 * A record to be used to use to define when a data object is relational and to where.
 * @param type the relation type
 * @param id the relation ID
 */
public record RelationalId(String type, String id) {

}
