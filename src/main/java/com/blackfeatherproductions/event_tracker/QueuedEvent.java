package com.blackfeatherproductions.event_tracker;

import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventPriority;

import io.vertx.core.json.JsonObject;

public class QueuedEvent
{
    private final EventPriority priority;

    private final Event event;
    private final JsonObject payload;
    private final Environment environment;

    public QueuedEvent(EventPriority priority, Event event, JsonObject payload, Environment environment)
    {
        this.priority = priority;
        this.event = event;
        this.payload = payload;
        this.environment = environment;
    }

    public void processEvent()
    {
        event.preProcessEvent(payload, environment);
    }

    public EventPriority getPriority()
    {
        return priority;
    }
}
