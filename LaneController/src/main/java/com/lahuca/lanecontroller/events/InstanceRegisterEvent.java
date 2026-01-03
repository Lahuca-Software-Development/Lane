package com.lahuca.lanecontroller.events;

import com.lahuca.lane.events.LaneEvent;
import com.lahuca.lane.events.LanePlayerEvent;
import com.lahuca.lanecontroller.ControllerLaneInstance;
import com.lahuca.lanecontroller.ControllerPlayer;

/**
 * This event is called when a new instance is registered.
 * @param instance the instance
 */
public record InstanceRegisterEvent(ControllerLaneInstance instance) implements LaneEvent {

}
