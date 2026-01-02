package com.lahuca.lane.events;

import com.lahuca.lane.game.LaneGame;

public interface LaneGameEvent<G extends LaneGame> extends LaneEvent {

    G getGame();

}
