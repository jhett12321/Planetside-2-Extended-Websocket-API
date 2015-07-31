package com.blackfeatherproductions.event_tracker.server.actions;

import java.util.Map.Entry;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.World;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

@ActionInfo(actionNames = "activeAlerts")
public class ActiveAlerts implements Action
{
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();

    @Override
    public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
    {
        JsonObject response = new JsonObject();
        response.put("action", "activeAlerts");

        JsonObject worlds = new JsonObject();

        if (actionData.containsKey("worlds"))
        {
            for (int i = 0; i < actionData.getJsonArray("worlds").size(); i++)
            {
                if (Utils.isValidWorld(actionData.getJsonArray("worlds").getString(i)))
                {
                    World world = World.getWorldByID(actionData.getJsonArray("worlds").getString(i));

                    JsonObject metagameEvents = new JsonObject();

                    for (MetagameEventInfo metagameEventInfo : dynamicDataManager.getWorldInfo(world).getActiveMetagameEvents().values())
                    {
                        JsonObject metagameEvent = new JsonObject();

                        metagameEvent.put("instance_id", metagameEventInfo.getInstanceID());
                        metagameEvent.put("metagame_event_type_id", metagameEventInfo.getType().getID());
                        metagameEvent.put("start_time", metagameEventInfo.getStartTime());
                        metagameEvent.put("end_time", metagameEventInfo.getEndTime());
                        metagameEvent.put("facility_type_id", metagameEventInfo.getType().getFacilityType().getID());
                        metagameEvent.put("category_id", metagameEventInfo.getType().getCategoryID());

                        JsonObject facilities = new JsonObject();

                        for (Entry<Facility, FacilityInfo> facilityInfo : dynamicDataManager.getWorldInfo(world).getZoneInfo(metagameEventInfo.getType().getZone()).getFacilities().entrySet())
                        {
                            JsonObject facility = new JsonObject();

                            facility.put("facility_id", facilityInfo.getKey().getID());
                            facility.put("facility_type_id", facilityInfo.getKey().getType().getID());
                            facility.put("owner", facilityInfo.getValue().getOwner().getID());
                            facility.put("zone_id", metagameEventInfo.getType().getZone().getID());

                            facilities.put(facilityInfo.getKey().getID(), facility);
                        }

                        metagameEvent.put("facilities", facilities);

                        metagameEvents.put(metagameEventInfo.getInstanceID(), metagameEvent);
                    }

                    if (metagameEvents.size() > 0)
                    {
                        worlds.put(world.getID(), metagameEvents);
                    }
                }
            }
        }
        else
        {
            for (Entry<World, WorldInfo> world : dynamicDataManager.getAllWorldInfo().entrySet())
            {
                JsonObject metagameEvents = new JsonObject();

                for (MetagameEventInfo metagameEventInfo : world.getValue().getActiveMetagameEvents().values())
                {
                    JsonObject metagameEvent = new JsonObject();

                    metagameEvent.put("instance_id", metagameEventInfo.getInstanceID());
                    metagameEvent.put("metagame_event_type_id", metagameEventInfo.getType().getID());
                    metagameEvent.put("start_time", metagameEventInfo.getStartTime());
                    metagameEvent.put("end_time", metagameEventInfo.getEndTime());
                    metagameEvent.put("facility_type_id", metagameEventInfo.getType().getFacilityType().getID());
                    metagameEvent.put("category_id", metagameEventInfo.getType().getCategoryID());

                    JsonObject facilities = new JsonObject();

                    for (Entry<Facility, FacilityInfo> facilityInfo : world.getValue().getZoneInfo(metagameEventInfo.getType().getZone()).getFacilities().entrySet())
                    {
                        JsonObject facility = new JsonObject();

                        facility.put("facility_id", facilityInfo.getKey().getID());
                        facility.put("facility_type_id", facilityInfo.getKey().getType().getID());
                        facility.put("owner", facilityInfo.getValue().getOwner().getID());
                        facility.put("zone_id", metagameEventInfo.getType().getZone().getID());

                        facilities.put(facilityInfo.getKey().getID(), facility);
                    }

                    metagameEvent.put("facilities", facilities);

                    metagameEvents.put(metagameEventInfo.getInstanceID(), metagameEvent);
                }

                worlds.put(world.getKey().getID(), metagameEvents);
            }
        }

        //Send Client Response
        response.put("worlds", worlds);

        clientConnection.writeFinalTextFrame(response.encode());
    }

}
