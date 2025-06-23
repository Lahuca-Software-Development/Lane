package com.lahuca.lane.game;

import com.lahuca.lane.LaneStateProperty;

import java.util.HashMap;
import java.util.Optional;

public interface LaneGame extends Slottable {

    long getGameId();
    String getInstanceId();
    String getGameType(); // Example: SkyWars
    Optional<String> getGameMode(); // Example: Singles
    Optional<String> getGameMap(); // Example: Village

    Optional<String> getState();
    HashMap<String, ? extends LaneStateProperty> getProperties();

}
