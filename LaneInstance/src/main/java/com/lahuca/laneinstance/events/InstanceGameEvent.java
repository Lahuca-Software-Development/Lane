package com.lahuca.laneinstance.events;

import com.lahuca.laneinstance.InstanceGame;

public interface InstanceGameEvent extends InstanceEvent {

    InstanceGame getGame();

}
