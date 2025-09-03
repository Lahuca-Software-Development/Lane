package com.lahuca.laneinstance.events;

import com.lahuca.laneinstance.game.InstanceGame;

public interface InstanceGameEvent extends InstanceEvent {

    InstanceGame getGame();

}
