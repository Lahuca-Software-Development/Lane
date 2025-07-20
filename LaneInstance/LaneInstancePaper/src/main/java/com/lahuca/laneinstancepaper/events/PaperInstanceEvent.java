package com.lahuca.laneinstancepaper.events;

import com.lahuca.laneinstance.events.InstanceEvent;
import org.bukkit.event.Event;

public abstract class PaperInstanceEvent<T extends InstanceEvent> extends Event {

    private final T instanceEvent;

    PaperInstanceEvent(T instanceEvent) {
        this.instanceEvent = instanceEvent;
    }

    PaperInstanceEvent(boolean isAsync, T instanceEvent) {
        super(isAsync);
        this.instanceEvent = instanceEvent;
    }

    public T getInstanceEvent() {
        return instanceEvent;
    }

}
