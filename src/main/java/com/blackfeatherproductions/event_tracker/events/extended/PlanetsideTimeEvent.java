package com.blackfeatherproductions.event_tracker.events.extended;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.queries.Environment;

@EventInfo(eventType = EventType.EVENT,
        eventName = "PlanetsideTime",
        listenedEvents = "PlanetsideTime",
        priority = EventPriority.NORMAL)
public class PlanetsideTimeEvent implements Event
{
    //Utils
    private final EventTracker eventTracker = EventTracker.getInstance();

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
        //Event Specific Data
        eventData.putString("old_time", payload.getString("old_time"));
        eventData.putString("new_time", payload.getString("new_time"));
        eventData.putString("diff", payload.getString("diff"));

        eventTracker.getEventServer().broadcastEvent(this);
    }
}
