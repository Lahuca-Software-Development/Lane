package com.lahuca.lane;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/

import java.util.HashMap;

/**
 * Interface representing a requested state.
 */
public interface LanePlayerState {

    /**
     * Gets the name of this requested state.
     *
     * @return the name of this requested state
     */
    String getName();

    /**
     * Gets the properties associated with this requested state.
     *
     * @return the properties associated with this requested state
     */
    HashMap<String, ? extends LaneStateProperty> getProperties();
}
