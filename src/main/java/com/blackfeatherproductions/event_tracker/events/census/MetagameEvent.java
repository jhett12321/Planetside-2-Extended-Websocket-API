package com.blackfeatherproductions.event_tracker.events.census;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.FacilityType;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.MetagameEventType;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.utils.TerritoryInfo;
import com.blackfeatherproductions.event_tracker.utils.TerritoryUtils;

@EventInfo(eventType = EventType.EVENT,
        eventName = "MetagameEvent",
        listenedEvents = "MetagameEvent|FacilityControl",
        priority = EventPriority.HIGH,
        filters =
        {
            "metagames", "metagame_types", "categories", "statuses", "dominations", "zones", "worlds"
        })
public class MetagameEvent implements Event
{
    //Utils
    private final DynamicDataManager dynamicDataManager = EventTracker.instance.getDynamicDataManager();

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
        String category_id;
        String start_time = "0";
        String end_time = "0";
        String status = null;
        String status_name = null;
        String domination = "0";

        Zone zone = Zone.getZoneByID(payload.getString("zone_id"));

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

            metagame_event_id = payload.getString("metagame_event_id");
            
            //Facility Object
            Facility facility = Facility.getFacilityByID(payload.getString("facility_id"));
            FacilityInfo facilityInfo = dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacility(facility);

            MetagameEventInfo metagameEventInfo = null;
            for (MetagameEventInfo info : worldData.getActiveMetagameEvents().values())
            {
                if(info.getType().getID().equals(metagame_event_id))
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
                category_id = metagameEventInfo.getType().getCategoryID();
                
                start_time = metagameEventInfo.getStartTime();
                end_time = metagameEventInfo.getEndTime();
                status = "2";
                status_name = "facility_update";
                domination = "0";

                //Facility Payload
                JsonObject facilityCaptured = new JsonObject();
                facilityCaptured.put("facility_id", facility.getID());
                facilityCaptured.put("facility_type_id", facility.getType().getID());
                facilityCaptured.put("owner", facilityInfo.getOwner().getID());
                facilityCaptured.put("zone_id", zone.getID());

                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacility(Facility.getFacilityByID(facility.getID())).setOwner(new_faction);
            }
        }

        //Raw Data - MetagameEvent
        else
        {
            MetagameEventType metagameEventType = MetagameEventType.getMetagameEventTypeByID(payload.getString("metagame_event_id"));

            instance_id = payload.getString("instance_id");
            metagame_event_id = metagameEventType.getID();
            category_id = metagameEventType.getCategoryID();

            //Alert End (138->ended, 137->canceled, 136->restarted)
            if (payload.getString("metagame_event_state").equals("138") || payload.getString("metagame_event_state").equals("137") || payload.getString("metagame_event_state").equals("136"))
            {
                MetagameEventInfo metagameEventInfo = worldData.getActiveMetagameEvent(instance_id);
                status = "0";
                status_name = "ended";

                //If this is a restart, metagameEventInfo will be null as we don't have a pre-existing metagame event.
                if (metagameEventInfo != null)
                {
                    start_time = metagameEventInfo.getStartTime();
                    end_time = timestamp;

                    //Remove event from tracking list.
                    worldData.removeMetagameEvent(instance_id);
                }
            }

            //Alert Start (135->started, 136->restarted)
            if (payload.getString("metagame_event_state").equals("135") || payload.getString("metagame_event_state").equals("136"))
            {
                start_time = timestamp;
                end_time = String.valueOf((Integer.parseInt(timestamp) + 7200));

                if (payload.getString("metagame_event_state").equals("136"))
                {
                    status = "3";
                    status_name = "restarted";
                }
                else
                {
                    status = "1";
                    status_name = "started";
                }

                //Create a new Metagame Event
                worldData.addMetagameEvent(instance_id, new MetagameEventInfo(instance_id, zone, metagameEventType, start_time, end_time));
            }
        }

        //Raw Data - Territory Control
        TerritoryInfo controlInfo = TerritoryUtils.calculateTerritoryControl(world, zone);
        String control_vs = String.valueOf(controlInfo.controlVS);
        String control_nc = String.valueOf(controlInfo.controlNC);
        String control_tr = String.valueOf(controlInfo.controlTR);

        String total_vs = String.valueOf(controlInfo.totalVS);
        String total_nc = String.valueOf(controlInfo.totalNC);
        String total_tr = String.valueOf(controlInfo.totalTR);

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
            eventData.put("category_id", category_id);
            eventData.put("status", status);
            eventData.put("status_name", status_name);
            eventData.put("control_vs", control_vs);
            eventData.put("control_nc", control_nc);
            eventData.put("control_tr", control_tr);
            eventData.put("total_vs", total_vs);
            eventData.put("total_nc", total_nc);
            eventData.put("total_tr", total_tr);
            eventData.put("domination", domination);
            eventData.put("zone_id", zone.getID());
            eventData.put("world_id", world.getID());

            //Filter Data		
            filterData.put("metagames", new JsonArray().add(instance_id));
            filterData.put("metagame_event_types", new JsonArray().add(metagame_event_id));
            filterData.put("categories", new JsonArray().add(category_id));
            filterData.put("statuses", new JsonArray().add(status));
            filterData.put("dominations", new JsonArray().add(domination));
            filterData.put("zones", new JsonArray().add(zone.getID()));
            filterData.put("worlds", new JsonArray().add(world.getID()));

            //Broadcast Event		
            EventTracker.instance.getEventServer().broadcastEvent(this);
        }
    }
}
