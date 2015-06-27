package com.blackfeatherproductions.event_tracker.events.census;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.MetagameEventType;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.Environment;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@EventInfo(eventType = EventType.EVENT,
        eventName = "MetagameEvent",
        listenedEvents = "MetagameEvent|FacilityControl",
        priority = EventPriority.HIGH,
        filters =
        {
            "metagames", "metagame_types", "facility_types", "statuses", "dominations", "zones", "worlds"
        })
public class MetagameEvent implements Event
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
        //Raw Data - Shared
        String event_name = payload.getString("event_name");
        String timestamp = payload.getString("timestamp");

        World world = World.getWorldByID(payload.getString("world_id"));
        WorldInfo worldData = dynamicDataManager.getWorldInfo(world);

        //Raw Data - To be resolved
        String instance_id;
        String metagame_event_id;
        String facility_type_id;
        String start_time = "0";
        String end_time = "0";
        String status = null;
        String domination = "0";

        Zone zone;

        //Raw Data - FacilityControl
        if (event_name.equals("FacilityControl"))
        {
            Faction new_faction = Faction.getFactionByID(payload.getString("new_faction_id"));
            Faction old_faction = Faction.getFactionByID(payload.getString("old_faction_id"));

            String is_capture = "0";
            if (!new_faction.equals(old_faction))
            {
                is_capture = "1";
            }

            if (!is_capture.equals("1"))
            {
                return;
            }

            zone = Zone.getZoneByID(payload.getString("zone_id"));

            MetagameEventInfo metagameEventInfo = null;
            for (MetagameEventInfo info : worldData.getActiveMetagameEvents().values())
            {
                if (info.getType().getZone() == zone)
                {
                    metagameEventInfo = info;
                }
            }

            if (metagameEventInfo == null)
            {
                return; //No alert is running in this zone.
            }

            else
            {
                instance_id = metagameEventInfo.getInstanceID();
                metagame_event_id = metagameEventInfo.getType().getID();
                facility_type_id = metagameEventInfo.getType().getFacilityTypeID();
                start_time = metagameEventInfo.getStartTime();
                end_time = metagameEventInfo.getEndTime();
                status = "2";
                domination = "0";

                //Facility Captured Object
                Facility facility = Facility.getFacilityByID(payload.getString("facility_id"));
                FacilityInfo facilityInfo = dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacility(facility);

                JsonObject facilityCaptured = new JsonObject();
                facilityCaptured.put("facility_id", facility.getID());
                facilityCaptured.put("facility_type_id", facility.getTypeID());
                facilityCaptured.put("owner", facilityInfo.getOwner().getID());
                facilityCaptured.put("zone_id", zone.getID());

                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacility(Facility.getFacilityByID(facility.getID())).setOwner(new_faction);
            }
        }

        //Raw Data - MetagameEvent
        else
        {
            MetagameEventType metagameEventType = MetagameEventType.getMetagameEventTypeByID(payload.getString("metagame_event_id"));

            zone = metagameEventType.getZone();

            instance_id = payload.getString("instance_id");
            metagame_event_id = metagameEventType.getID();
            facility_type_id = metagameEventType.getFacilityTypeID();

            //Alert End (138->ended, 137->canceled, 136->restarted)
            if (payload.getString("metagame_event_state").equals("138") || payload.getString("metagame_event_state").equals("137") || payload.getString("metagame_event_state").equals("136"))
            {
                MetagameEventInfo metagameEventInfo = worldData.getActiveMetagameEvent(instance_id);

                //If this is a restart, metagameEventInfo will be null as we don't have a pre-existing metagame event.
                if(metagameEventInfo != null)
                {
                    start_time = metagameEventInfo.getStartTime();
                    end_time = timestamp;
                    status = "0";

                    //Remove event from tracking list.
                    worldData.removeMetagameEvent(instance_id);
                }
            }
            
            //Alert Start (135->started, 136->restarted)
            if (payload.getString("metagame_event_state").equals("135") || payload.getString("metagame_event_state").equals("136"))
            {
                start_time = timestamp;
                end_time = String.valueOf((Integer.parseInt(timestamp) + 7200));
                status = "1";

                //Create a new Metagame Event
                worldData.addMetagameEvent(instance_id, new MetagameEventInfo(instance_id, metagameEventType, start_time, end_time));
            }
        }

        //Raw Data - Territory Control
        JsonObject controlInfo = Utils.calculateTerritoryControl(world, zone);
        String control_vs = controlInfo.getString("control_vs");
        String control_nc = controlInfo.getString("control_nc");
        String control_tr = controlInfo.getString("control_tr");

        if (Integer.parseInt(control_vs) >= 100 || Integer.parseInt(control_nc) >= 100 || Integer.parseInt(control_tr) >= 100)
        {
            domination = "1";
        }

        //Event Data
        boolean isDummy = payload.containsKey("is_dummy") && payload.getString("is_dummy").equals("1");

        if (!isDummy)
        {
            eventData.put("instance_id", instance_id);
            eventData.put("metagame_event_type_id", metagame_event_id);
            eventData.put("start_time", start_time);
            eventData.put("end_time", end_time);
            eventData.put("timestamp", timestamp);
            eventData.put("facility_type_id", facility_type_id);
            eventData.put("status", status);
            eventData.put("control_vs", control_vs);
            eventData.put("control_nc", control_nc);
            eventData.put("control_tr", control_tr);
            eventData.put("domination", domination);
            eventData.put("zone_id", zone.getID());
            eventData.put("world_id", world.getID());

            //Filter Data		
            filterData.put("metagames", new JsonArray().add(instance_id));
            filterData.put("metagame_event_types", new JsonArray().add(metagame_event_id));
            filterData.put("facility_types", new JsonArray().add(facility_type_id));
            filterData.put("statuses", new JsonArray().add(status));
            filterData.put("dominations", new JsonArray().add(domination));
            filterData.put("zones", new JsonArray().add(zone.getID()));
            filterData.put("worlds", new JsonArray().add(world.getID()));

            //Broadcast Event		
            EventTracker.getEventServer().broadcastEvent(this);
        }
    }
}
