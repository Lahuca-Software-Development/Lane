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
     * Properties set: instanceId -> String, timestamp -> long
     */
    String INSTANCE_TRANSFER = "instance.transfer";
    /**
     * Constant for the state name of a player trying to join a game (therefore sub of {@link #INSTANCE_TRANSFER}).
     * Properties set: gameId -> Long, instanceId -> String, timestamp -> long
     */
    String GAME_TRANSFER = "game.transfer";
    /**
     * Constant for the state name of a player that has joined an instance, but the data has not yet been retrieved by the instance.
     * Properties set: instanceId -> String, timestamp -> long
     */
    String INSTANCE_TRANSFERRED = "instance.transferred";
    /**
     * Constant for the state name of a player that has joined a game (therefore sub of {@link #INSTANCE_TRANSFERRED}), but the data has not yet been retrieved by the instance.
     * Properties set: instanceId -> String, timestamp -> long
     */
    String GAME_TRANSFERRED = "game.transferred";
    /**
     * Constant for the state name of a player that is online on an instance.
     * Properties set: instanceId -> String
     */
    String INSTANCE_ONLINE = "instance.online";
    /**
     * Constant for the state name of a player that is online on a game (therefore sub of {@link #INSTANCE_ONLINE}.
     * Properties set: gameId -> Long, instanceId -> String
     */
    String GAME_ONLINE = "game.online";
    /**
     * Constant for when the instance has retrieved a disconnect, but there is not yet a connect state change.
     * Properties set: timestamp -> long
     */
    String OFFLINE = "offline";


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
