package com.blackfeatherproductions.event_tracker;

import java.util.Date;

import com.blackfeatherproductions.event_tracker.data_static.Environment;

import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventPriority;

public class QueuedEvent
{
    private final EventPriority priority;

    private final Event event;
    private final JsonObject payload;
    private final Environment environment;
    
    private final Integer eventTimestamp;
    private final Long creationTimestamp = new Date().getTime();

    public QueuedEvent(EventPriority priority, Event event, JsonObject payload, Environment environment)
    {
        this.priority = priority;
        this.event = event;
        this.payload = payload;
        this.environment = environment;
        
        this.eventTimestamp = payload.containsKey("timestamp") ? Integer.valueOf(payload.getString("timestamp")) : null;
    }

    public void processEvent()
    {
        event.preProcessEvent(payload, environment);
    }

    public EventPriority getPriority()
    {
        return priority;
    }
    
    public Integer getEventTimestamp()
    {
        return eventTimestamp;
    }
    
    public Long getCreationTimestamp()
    {
        return creationTimestamp;
    }
}
