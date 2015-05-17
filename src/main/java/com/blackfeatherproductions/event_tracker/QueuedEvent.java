package com.blackfeatherproductions.event_tracker;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventPriority;

public class QueuedEvent
{
    private final EventPriority priority;

    private final Event event;
    private final JsonObject payload;

    public QueuedEvent(EventPriority priority, Event event, JsonObject payload)
    {
        this.priority = priority;
        this.event = event;
        this.payload = payload;
    }

    public void processEvent()
    {
        event.preProcessEvent(payload);
    }

    public EventPriority getPriority()
    {
        return priority;
    }
}
