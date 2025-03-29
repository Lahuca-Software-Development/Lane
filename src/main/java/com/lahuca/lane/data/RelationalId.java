package com.lahuca.lane.data;

/**
 * A record to be used to use to define when a data object is relational and to where.
 * @param type the relation type, must only contain letters from the alphabet with a minimum length of 1 character and maximum length of 64.
 * @param id the relation ID
 */
public record RelationalId(String type, String id) {

    public static RelationalId Players(String id) {
        return new RelationalId("players", id);
    }

}
