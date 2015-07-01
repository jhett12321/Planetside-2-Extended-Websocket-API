package com.blackfeatherproductions.event_tracker.events.census;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.QueryManager;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;

@EventInfo(eventType = EventType.EVENT,
        eventName = "ContinentLock",
        listenedEvents = "ContinentLock",
        priority = EventPriority.LOWEST,
        filters =
        {
            "factions", "zones", "worlds"
        })
public class ContinentLockEvent implements Event
{
    //Utils
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();
    private final QueryManager queryManager = EventTracker.getQueryManager();

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
        //Raw Data
        String vs_population = payload.getString("vs_population");
        String nc_population = payload.getString("nc_population");
        String tr_population = payload.getString("tr_population");

        Faction locked_by = Faction.getFactionByID(payload.getString("triggering_faction"));
        String metagame_event_id = payload.getString("metagame_event_id");

        String timestamp = payload.getString("timestamp");
        Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
        World world = World.getWorldByID(payload.getString("world_id"));

        //Update Internal Data
        dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLocked(true);
        dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLockingFaction(locked_by);
        
        //Re-sync territory control
        //TODO remove if no-longer required
        queryManager.queryWorld(world.getID(), environment);

        //Event Data
        eventData.put("vs_population", vs_population);
        eventData.put("nc_population", nc_population);
        eventData.put("tr_population", tr_population);

        eventData.put("locked_by", locked_by.getID());
        eventData.put("metagame_event_id", metagame_event_id);

        eventData.put("timestamp", timestamp);
        eventData.put("zone_id", zone.getID());
        eventData.put("world_id", world.getID());

        //Filter Data
        filterData.put("factions", new JsonArray().add(locked_by.getID()));
        filterData.put("zones", new JsonArray().add(zone.getID()));
        filterData.put("worlds", new JsonArray().add(world.getID()));

        //Broadcast Event
        EventTracker.getEventServer().broadcastEvent(this);
    }
}
