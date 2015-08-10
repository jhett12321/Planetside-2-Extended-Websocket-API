package com.blackfeatherproductions.event_tracker.events.census;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.utils.TerritoryUtils;

@EventInfo(eventType = EventType.EVENT,
        eventName = "FacilityControl",
        listenedEvents = "FacilityControl",
        priority = EventPriority.NORMAL,
        filters =
        {
            "facilties", "facility_types", "outfits", "factions", "captures", "zones", "worlds"
        })
public class FacilityControlEvent implements Event
{
    //Utils
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();

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
        String facility_id = payload.getString("facility_id");
        Facility facility = Facility.getFacilityByID(facility_id);
        String outfit_id = payload.getString("outfit_id");
        String duration_held = payload.getString("duration_held");

        Faction new_faction = Faction.getFactionByID(payload.getString("new_faction_id"));
        Faction old_faction = Faction.getFactionByID(payload.getString("old_faction_id"));

        String is_capture = !new_faction.equals(old_faction) ? "1" : "0";

        String timestamp = payload.getString("timestamp");
        Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
        World world = World.getWorldByID(payload.getString("world_id"));

        //Update Internal Data
        if (is_capture.equals("1"))
        {
            dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacility(Facility.getFacilityByID(facility_id)).setOwner(new_faction);
        }

        //Territory Control
        JsonObject controlInfo = TerritoryUtils.calculateTerritoryControl(world, zone);
        String control_vs = controlInfo.getString("control_vs");
        String control_nc = controlInfo.getString("control_nc");
        String control_tr = controlInfo.getString("control_tr");

        //Event Data
        eventData.put("facility_id", facility_id);
        eventData.put("facility_type_id", facility.getType().getID());
        eventData.put("outfit_id", outfit_id);
        eventData.put("duration_held", duration_held);

        eventData.put("new_faction_id", new_faction.getID());
        eventData.put("old_faction_id", old_faction.getID());

        eventData.put("is_capture", is_capture);

        eventData.put("control_vs", control_vs);
        eventData.put("control_nc", control_nc);
        eventData.put("control_tr", control_tr);

        eventData.put("timestamp", timestamp);
        eventData.put("zone_id", zone.getID());
        eventData.put("world_id", world.getID());

        //Filter Data
        filterData.put("facilities", new JsonArray().add(facility_id));
        filterData.put("facility_types", new JsonArray().add(facility.getType().getID()));
        filterData.put("outfits", new JsonArray().add(outfit_id));
        filterData.put("factions", new JsonArray().add(new_faction.getID()).add(old_faction.getID()));
        filterData.put("captures", new JsonArray().add(is_capture));
        filterData.put("zones", new JsonArray().add(zone.getID()));
        filterData.put("worlds", new JsonArray().add(world.getID()));

        //Broadcast Event
        EventTracker.getEventServer().broadcastEvent(this);
    }
}
