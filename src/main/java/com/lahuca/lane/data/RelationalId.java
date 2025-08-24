package com.lahuca.lane.data;

import java.util.UUID;

/**
 * A record to be used to use to define when a data object is relational and to where.
 * @param type the relation type, must only contain letters from the alphabet with a minimum length of 1 character and maximum length of 64.
 * @param id the relation ID
 */
public record RelationalId(String type, String id) {

    /**
     * Constructs a RelationalId where the type is of players.
     * The players relational table is to be used to store information about a single player.
     * @param uuid the player UUID
     * @return the RelationalId
     */
    public static RelationalId Players(UUID uuid) {
        return new RelationalId("players", uuid.toString());
    }

    /**
     * Constructs a RelationalId where the type is of friendships.
     * The friendships relational table is to be used to store information about friendships (/friends).
     * @param friendshipId the friendship ID
     * @return the RelationalID
     */
    public static RelationalId Friendships(long friendshipId) {
        return new RelationalId("friendships", Long.toString(friendshipId));
    }

    /**
     * Constructs a RelationalId where the type is of usernames.
     * The usernames relational table is to be used to store information about usernames.
     * Here usernames can be transferred to UUIDs for offline players.
     * @param username the username
     * @return the RelationalID
     */
    public static RelationalId Usernames(String username) {
        return new RelationalId("usernames", username);
    }

    /**
     * Constructs a RelationalId where the type is of profiles.
     * The profiles relational table is to be used to store information about a profile.
     * @param uuid the profile UUID
     * @return the RelationalId
     */
    public static RelationalId Profiles(UUID uuid) {
        return new RelationalId("profiles", uuid.toString());
    }

}
