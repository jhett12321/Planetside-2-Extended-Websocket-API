package com.blackfeatherproductions.event_tracker.server.actions;

import java.util.Map.Entry;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

@ActionInfo(actionNames = "facilityStatus")
public class FacilityStatus implements Action
{
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();

    @Override
    public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
    {
        JsonObject response = new JsonObject();
        response.put("action", "facilityStatus");

        if (actionData.containsKey("worlds") && actionData.getJsonArray("worlds").size() > 0 && actionData.containsKey("zones") && actionData.getJsonArray("zones").size() > 0)
        {
            JsonObject worlds = new JsonObject();

            for (int i = 0; i < actionData.getJsonArray("worlds").size(); i++)
            {
                if (Utils.isValidWorld(actionData.getJsonArray("worlds").getString(i)))
                {
                    World world = World.getWorldByID(actionData.getJsonArray("worlds").getString(i));

                    JsonObject worldObj = new JsonObject();
                    JsonObject zones = new JsonObject();

                    for (int j = 0; j < actionData.getJsonArray("zones").size(); j++)
                    {
                        if (Utils.isValidZone(actionData.getJsonArray("zones").getString(j)))
                        {
                            Zone zone = Zone.getZoneByID(actionData.getJsonArray("zones").getString(j));

                            JsonObject facilities = new JsonObject();

                            for (Entry<Facility, FacilityInfo> facilityInfo : dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacilities().entrySet())
                            {
                                JsonObject facility = new JsonObject();

                                facility.put("facility_id", facilityInfo.getKey().getID());
                                facility.put("facility_type_id", facilityInfo.getKey().getTypeID());
                                facility.put("owner", facilityInfo.getValue().getOwner().getID());
                                facility.put("zone_id", zone.getID());

                                facilities.put(facilityInfo.getKey().getID(), facility);
                            }

                            zones.put(zone.getID(), facilities);
                        }
                    }

                    worldObj.put("zones", zones);
                    worlds.put(world.getID(), worldObj);
                }
            }

            response.put("worlds", worlds);
        }

        else
        {
            response.put("error", "MissingFilters");
            response.put("message", "You are missing required filters for this action. Please check your syntax and try again.");
        }

        clientConnection.writeFinalTextFrame(response.encode());
    }
}
