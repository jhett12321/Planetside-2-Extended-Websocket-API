package com.blackfeatherproductions.event_tracker.server.actions;

import java.util.Map.Entry;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.utils.CensusUtils;
import com.blackfeatherproductions.event_tracker.utils.TerritoryUtils;

@ActionInfo(actionNames = "activeMetagameEvents")
public class ActiveMetagameEvents implements Action
{
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();

    @Override
    public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
    {
        JsonObject response = new JsonObject();
        response.put("action", "activeMetagameEvents");

        JsonObject worlds = new JsonObject();

        if (actionData.containsKey("worlds"))
        {
            for (int i = 0; i < actionData.getJsonArray("worlds").size(); i++)
            {
                if (CensusUtils.isValidWorld(actionData.getJsonArray("worlds").getString(i)))
                {
                    World world = World.getWorldByID(actionData.getJsonArray("worlds").getString(i));
                    
                    if(dynamicDataManager.getWorldInfo(world).getActiveMetagameEvents().size() > 0)
                    {
                        JsonObject worldObj = new JsonObject();
                        
                        JsonArray metagameEvents = new JsonArray();

                        for (MetagameEventInfo metagameEventInfo : dynamicDataManager.getWorldInfo(world).getActiveMetagameEvents().values())
                        {
                            JsonObject metagameEvent = new JsonObject();

                            metagameEvent.put("instance_id", metagameEventInfo.getInstanceID());
                            metagameEvent.put("metagame_event_type_id", metagameEventInfo.getType().getID());
                            metagameEvent.put("start_time", metagameEventInfo.getStartTime());
                            metagameEvent.put("end_time", metagameEventInfo.getEndTime());
                            metagameEvent.put("facility_type_id", metagameEventInfo.getType().getFacilityType().getID());
                            metagameEvent.put("category_id", metagameEventInfo.getType().getCategoryID());
                            
                            JsonObject controlInfo = TerritoryUtils.calculateTerritoryControl(world, metagameEventInfo.getType().getZone());
                            String control_vs = controlInfo.getString("control_vs");
                            String control_nc = controlInfo.getString("control_nc");
                            String control_tr = controlInfo.getString("control_tr");
                            
                            metagameEvent.put("control_vs", control_vs);
                            metagameEvent.put("control_nc", control_nc);
                            metagameEvent.put("control_tr", control_tr);

                            JsonArray facilities = new JsonArray();

                            for (Entry<Facility, FacilityInfo> facilityInfo : dynamicDataManager.getWorldInfo(world).getZoneInfo(metagameEventInfo.getType().getZone()).getFacilities().entrySet())
                            {
                                JsonObject facility = new JsonObject();

                                facility.put("facility_id", facilityInfo.getKey().getID());
                                facility.put("facility_type_id", facilityInfo.getKey().getType().getID());
                                facility.put("owner", facilityInfo.getValue().getOwner().getID());
                                facility.put("zone_id", metagameEventInfo.getType().getZone().getID());

                                facilities.add(facility);
                            }

                            metagameEvent.put("facilities", facilities);

                            metagameEvents.add(metagameEvent);
                        }
                        
                        worldObj.put("metagame_events", metagameEvents);

                        worlds.put(world.getID(), worldObj);
                    }
                }
            }
        }
        
        else
        {
            for (Entry<World, WorldInfo> world : dynamicDataManager.getAllWorldInfo().entrySet())
            {
                if(world.getValue().getActiveMetagameEvents().size() > 0)
                {
                    JsonObject worldObj = new JsonObject();
                    
                    JsonArray metagameEvents = new JsonArray();

                    for (MetagameEventInfo metagameEventInfo : world.getValue().getActiveMetagameEvents().values())
                    {
                        JsonObject metagameEvent = new JsonObject();

                        metagameEvent.put("instance_id", metagameEventInfo.getInstanceID());
                        metagameEvent.put("metagame_event_type_id", metagameEventInfo.getType().getID());
                        metagameEvent.put("start_time", metagameEventInfo.getStartTime());
                        metagameEvent.put("end_time", metagameEventInfo.getEndTime());
                        metagameEvent.put("facility_type_id", metagameEventInfo.getType().getFacilityType().getID());
                        metagameEvent.put("category_id", metagameEventInfo.getType().getCategoryID());
                        
                        JsonObject controlInfo = TerritoryUtils.calculateTerritoryControl(world.getKey(), metagameEventInfo.getType().getZone());
                        String control_vs = controlInfo.getString("control_vs");
                        String control_nc = controlInfo.getString("control_nc");
                        String control_tr = controlInfo.getString("control_tr");

                        metagameEvent.put("control_vs", control_vs);
                        metagameEvent.put("control_nc", control_nc);
                        metagameEvent.put("control_tr", control_tr);

                        JsonArray facilities = new JsonArray();

                        for (Entry<Facility, FacilityInfo> facilityInfo : world.getValue().getZoneInfo(metagameEventInfo.getType().getZone()).getFacilities().entrySet())
                        {
                            JsonObject facility = new JsonObject();

                            facility.put("facility_id", facilityInfo.getKey().getID());
                            facility.put("facility_type_id", facilityInfo.getKey().getType().getID());
                            facility.put("owner", facilityInfo.getValue().getOwner().getID());
                            facility.put("zone_id", metagameEventInfo.getType().getZone().getID());

                            facilities.add(facility);
                        }

                        metagameEvent.put("facilities", facilities);

                        metagameEvents.add(metagameEvent);
                    }

                    worldObj.put("metagame_events", metagameEvents);

                    worlds.put(world.getKey().getID(), worldObj);
                }
            }
        }

        //Send Client Response
        response.put("worlds", worlds);

        clientConnection.writeFinalTextFrame(response.encode());
    }

}
