package com.blackfeatherproductions.event_tracker.events.service;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;

@EventInfo(eventType = EventType.SERVICE,
        eventName = "ServiceStateChange",
        listenedEvents = "ServiceStateChange",
        priority = EventPriority.HIGHEST)
public class ServiceStateChangeEvent implements Event
{
    private final EventTracker eventTracker = EventTracker.getInstance();

    private JsonObject payload;

    @Override
    public void preProcessEvent(JsonObject payload)
    {
        this.payload = payload;
        if (payload != null)
        {
            processEvent();
        }
    }

    @Override
    public void processEvent()
    {
        JsonObject eventData = new JsonObject();

        //Event Specific Data
        eventData.putString("online", payload.getString("online"));
        eventData.putString("world_id", payload.getString("world_id"));

        JsonObject message = new JsonObject();
        message.putObject("event_data", eventData);

        eventTracker.getEventServer().broadcastEvent(this.getClass(), message);
    }
}
