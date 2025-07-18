package com.lahuca.laneinstance.events;

import com.lahuca.laneinstance.InstancePlayer;

public interface InstancePlayerEvent extends InstanceEvent {

    InstancePlayer getPlayer();

}
