package com.lahuca.lanecontroller.events;

import com.lahuca.lane.events.LaneEvent;
import com.lahuca.lanecontroller.ControllerLaneInstance;

/**
 * This event is called when a new instance is unregistered.
 * @param instance the instance
 */
public record InstanceUnregisterEvent(ControllerLaneInstance instance) implements LaneEvent {

}
