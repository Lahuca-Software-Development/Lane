package com.lahuca.lanecontrollervelocity;

import com.lahuca.lanecontroller.ControllerPlayer;
import com.velocitypowered.api.proxy.Player;

import java.util.UUID;

/**
 * A record containing the pair of a {@link com.lahuca.lanecontroller.Controller} and its attached {@link ControllerPlayer}.
 * @param player the Velocity player
 * @param cPlayer the Controller player
 */
public record VelocityPlayerPair(Player player, ControllerPlayer cPlayer) {

    /**
     * Construction of a new pair, both objects need to have the same uuid.
     * @param player the player
     * @param cPlayer the controller player
     */
    public VelocityPlayerPair {
        if (player == null) {
            throw new NullPointerException("player cannot be null");
        }
        if (cPlayer == null) {
            throw new NullPointerException("cPlayer cannot be null");
        }
        if (!player.getUniqueId().equals(cPlayer.getUuid())) {
            throw new IllegalArgumentException("player uuid is not the same as the cPlayer uuid");
        }
    }

    public UUID getUuid() {
        return cPlayer.getUuid();
    }

}
