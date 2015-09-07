package com.blackfeatherproductions.event_tracker.events.extended;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;

@EventInfo(eventType = EventType.EVENT,
        eventName = "PopulationChange",
        listenedEvents = "PopulationChange",
        priority = EventPriority.NORMAL,
        filters =
        {
            "population_types", "outfits", "zones", "worlds"
        })
public class PopulationChangeEvent implements Event
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

        processEvent();
    }

    @Override
    public void processEvent()
    {
        //Event/Filter Data
        String populationType = payload.getString("population_type");

        eventData.put("population_type", populationType);
        filterData.put("population_types", new JsonArray().add(populationType));

        eventData.put("population_total", payload.getString("population_total"));

        if (populationType.equals("total") || populationType.equals("world") || populationType.equals("zone"))
        {
            eventData.put("population_vs", payload.getString("population_vs"));
            eventData.put("population_nc", payload.getString("population_nc"));
            eventData.put("population_tr", payload.getString("population_tr"));
            eventData.put("population_unk", payload.getString("population_unk"));
        }

        if (populationType.equals("outfit") || populationType.equals("zone_outfit"))
        {
            eventData.put("outfit_id", payload.getString("outfit_id"));
            filterData.put("outfits", new JsonArray().add(payload.getString("outfit_id")));
        }

        if (populationType.equals("zone") || populationType.equals("zone_outfit"))
        {
            eventData.put("zone_id", payload.getString("zone_id"));
            filterData.put("zones", new JsonArray().add(payload.getString("zone_id")));
        }

        if (populationType.equals("world") || populationType.equals("zone") || populationType.equals("outfit") || populationType.equals("zone_outfit"))
        {
            eventData.put("world_id", payload.getString("world_id"));
            filterData.put("worlds", new JsonArray().add(payload.getString("world_id")));
        }

        //Broadcast Event
        EventTracker.getEventServer().broadcastEvent(this);
    }
}
