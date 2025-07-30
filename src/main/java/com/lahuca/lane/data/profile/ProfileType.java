package com.lahuca.lane.data.profile;

/**
 * An enum with the different profile types.
 */
public enum ProfileType {

    /**
     * The network profile type: the main profile of a player.
     */
    NETWORK,
    /**
     * The sub profile type: a sub profile can be attached to one another profile with a name.
     */
    SUB,
    /**
     * The shared profile type: a shared profile can be attached to multiple other profiles with different names.
     */
    SHARED

}
