package com.lahuca.laneinstancepaper.events;

import com.lahuca.lane.events.LaneEvent;
import org.bukkit.event.Event;

public abstract class PaperInstanceEvent<T extends LaneEvent> extends Event {

    private final T instanceEvent;

    public PaperInstanceEvent(T instanceEvent) {
        this.instanceEvent = instanceEvent;
    }

    public PaperInstanceEvent(boolean isAsync, T instanceEvent) {
        super(isAsync);
        this.instanceEvent = instanceEvent;
    }

    public T getInstanceEvent() {
        return instanceEvent;
    }

}
