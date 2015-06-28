package com.blackfeatherproductions.event_tracker.events.service;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.Environment;

import io.vertx.core.json.JsonObject;

@EventInfo(eventType = EventType.SERVICE,
        eventName = "ServiceStateChange",
        listenedEvents = "ServiceStateChange",
        priority = EventPriority.HIGHEST)
public class ServiceStateChangeEvent implements Event
{
    //Raw Data
    private JsonObject payload;

    //Message Data
    private JsonObject eventData = new JsonObject();
    private JsonObject filterData = new JsonObject();
    private Environment environment;

    @Override
    public Environment getEnvironment()
    {
        return environment;
    }

    @Override
    public JsonObject getEventData()
    {
        return eventData;
    }

    @Override
    public JsonObject getFilterData()
    {
        return filterData;
    }

    @Override
    public void preProcessEvent(JsonObject payload, Environment environment)
    {
        this.payload = payload;
        this.environment = environment;

        if (payload != null)
        {
            processEvent();
        }
    }

    @Override
    public void processEvent()
    {
        //Event Data
        eventData.put("online", payload.getString("online"));
        eventData.put("world_id", payload.getString("world_id"));

        //Broadcast Event
        EventTracker.getEventServer().broadcastEvent(this);
    }
}
