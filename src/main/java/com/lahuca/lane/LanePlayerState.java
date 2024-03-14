package com.lahuca.lane;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/

import java.util.HashMap;

/**
 * Interface representing a player state.
 */
public interface LanePlayerState {

    /**
     * Gets the name of this player state.
     *
     * @return the name of this player state
     */
    String getName();

    /**
     * Gets the properties associated with this player state.
     *
     * @return the properties associated with this player state
     */
    HashMap<String, LaneStateProperty> getProperties();
}
