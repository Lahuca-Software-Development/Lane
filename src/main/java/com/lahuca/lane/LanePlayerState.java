package com.lahuca.lane;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/

import com.lahuca.lane.records.PlayerStateRecord;

import java.util.HashMap;

/**
 * Interface representing a player state.
 */
public interface LanePlayerState extends RecordApplier<PlayerStateRecord> {


    /**
     * Constant for the state name of an undefined state.
     */
    String UNDEFINED = "undefined";
    /**
     * Constant for the state name of a player trying to join an instance.
     * Properties set: instanceId -> String
     */
    String INSTANCE_JOIN = "instance.join";
    /**
     * Constant for the state name of a player trying to join a game (therefore sub of {@link #INSTANCE_JOIN}).
     * Properties set: gameId -> Long, instanceId -> String
     */
    String GAME_JOIN = "game.join";

    /**
     * Gets the name of the state.
     *
     * @return the name of the state
     */
    String getName();

    /**
     * Gets the properties associated with the state.
     *
     * @return the properties associated with the state
     */
    HashMap<String, ? extends LaneStateProperty> getProperties();

}
