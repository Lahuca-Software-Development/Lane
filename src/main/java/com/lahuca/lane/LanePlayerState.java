package com.lahuca.lane;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/

import com.lahuca.lane.records.PlayerStateRecord;
import com.lahuca.lane.records.RecordConverterApplier;

import java.util.HashMap;

/**
 * Interface representing a player state.
 */
public interface LanePlayerState extends RecordConverterApplier<PlayerStateRecord> {


    /**
     * Constant for the state name of an undefined state.
     */
    String UNDEFINED = "UNDEFINED";
    /**
     * Constant for the state name of a player trying to join an instance.
     * Properties set: instanceId -> String, timestamp -> long
     */
    String INSTANCE_TRANSFER = "INSTANCE_TRANSFER";
    /**
     * Constant for the state name of a player trying to join a game (therefore sub of {@link #INSTANCE_TRANSFER}).
     * Properties set: gameId -> Long, instanceId -> String, timestamp -> long
     */
    String GAME_TRANSFER = "GAME_TRANSFER";
    /**
     * Constant for the state name of a player that is online on an instance.
     * Properties set: instanceId -> String
     */
    String INSTANCE_ONLINE = "INSTANCE_ONLINE";
    /**
     * Constant for the state name of a player that is online on a game (therefore sub of {@link #INSTANCE_ONLINE}.
     * Properties set: gameId -> Long, instanceId -> String
     */
    String GAME_ONLINE = "GAME_ONLINE";
    /**
     * Constant for when the instance has retrieved a disconnect, but there is not yet a connect state change.
     * Properties set: timestamp -> long
     */
    String OFFLINE = "OFFLINE";


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
