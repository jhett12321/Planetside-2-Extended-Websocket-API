package com.blackfeatherproductions.event_tracker.events.census;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_dynamic.ZoneInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.utils.TerritoryInfo;
import com.blackfeatherproductions.event_tracker.utils.TerritoryUtils;

@EventInfo(eventType = EventType.EVENT,
        eventName = "FacilityControl",
        listenedEvents = "FacilityControl",
        priority = EventPriority.NORMAL,
        filters =
        {
            "facilties", "facility_types", "outfits", "factions", "captures", "blocks", "zones", "worlds"
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
        String isBlockedUpdate = payload.containsKey("is_blocked_update") ? "1" : "0";
        
        String facility_id = payload.getString("facility_id");
        Facility facility = Facility.getFacilityByID(facility_id);
        String outfit_id = payload.getString("outfit_id");

        Faction new_faction = Faction.getFactionByID(payload.getString("new_faction_id"));
        Faction old_faction = Faction.getFactionByID(payload.getString("old_faction_id"));

        String is_capture = !new_faction.equals(old_faction) ? "1" : "0";

        String timestamp = payload.getString("timestamp");
        Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
        World world = World.getWorldByID(payload.getString("world_id"));
        
        ZoneInfo zoneInfo = dynamicDataManager.getWorldInfo(world).getZoneInfo(zone);

        //Update Internal Data
        if (is_capture.equals("1") && isBlockedUpdate.equals("0"))
        {
            zoneInfo.getFacility(Facility.getFacilityByID(facility_id)).setOwner(new_faction);

            TerritoryUtils.updateFacilityBlockedStatus(environment, world, zone, timestamp);
        }
        
        else if(isBlockedUpdate.equals("1"))
        {
            is_capture = "-1";
        }

        //Facility Blocked State
        String blocked = zoneInfo.getFacility(facility).isBlocked() ? "1" : "0";

        //Event Data
        eventData.put("facility_id", facility_id);
        eventData.put("facility_type_id", facility.getType().getID());
        eventData.put("outfit_id", outfit_id);

        eventData.put("new_faction_id", new_faction.getID());
        eventData.put("old_faction_id", old_faction.getID());

        eventData.put("is_capture", is_capture);
        
        if(payload.containsKey("duration_held"))
        {
            eventData.put("duration_held", payload.getString("duration_held"));
        }
        
        eventData.put("is_block_update", isBlockedUpdate);
        
        eventData.put("blocked", blocked);
        
        //Territory Control
        if(isBlockedUpdate.equals("0"))
        {
            TerritoryInfo controlInfo = TerritoryUtils.calculateTerritoryControl(world, zone);
            
            String control_vs = String.valueOf(controlInfo.controlVS);
            String control_nc = String.valueOf(controlInfo.controlNC);
            String control_tr = String.valueOf(controlInfo.controlTR);

            String total_vs = String.valueOf(controlInfo.totalVS);
            String total_nc = String.valueOf(controlInfo.totalNC);
            String total_tr = String.valueOf(controlInfo.totalTR);
            
            eventData.put("control_vs", control_vs);
            eventData.put("control_nc", control_nc);
            eventData.put("control_tr", control_tr);

            eventData.put("total_vs", total_vs);
            eventData.put("total_nc", total_nc);
            eventData.put("total_tr", total_tr);
        }

        eventData.put("timestamp", timestamp);
        eventData.put("zone_id", zone.getID());
        eventData.put("world_id", world.getID());

        //Filter Data
        filterData.put("facilities", new JsonArray().add(facility_id));
        filterData.put("facility_types", new JsonArray().add(facility.getType().getID()));
        filterData.put("outfits", new JsonArray().add(outfit_id));
        filterData.put("factions", new JsonArray().add(new_faction.getID()).add(old_faction.getID()));
        filterData.put("captures", new JsonArray().add(is_capture));
        filterData.put("blocks", new JsonArray().add(isBlockedUpdate));
        filterData.put("zones", new JsonArray().add(zone.getID()));
        filterData.put("worlds", new JsonArray().add(world.getID()));

        //Broadcast Event
        EventTracker.getEventServer().broadcastEvent(this);
    }
}
