package com.blackfeatherproductions.event_tracker.server.actions;

import java.util.Map.Entry;

import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

@ActionInfo(actionNames = "facilityStatus")
public class FacilityStatus implements Action
{
    private DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();

    @Override
    public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
    {
        JsonObject response = new JsonObject();
        response.putString("action", "facilityStatus");

        if (actionData.containsField("worlds") && actionData.getArray("worlds").size() > 0 && actionData.containsField("zones") && actionData.getArray("zones").size() > 0)
        {
            JsonObject worlds = new JsonObject();

            for (int i = 0; i < actionData.getArray("worlds").size(); i++)
            {
                if (Utils.isValidWorld((String) actionData.getArray("worlds").get(i)))
                {
                    World world = World.getWorldByID((String) actionData.getArray("worlds").get(i));

                    JsonObject worldObj = new JsonObject();
                    JsonObject zones = new JsonObject();

                    for (int j = 0; j < actionData.getArray("zones").size(); j++)
                    {
                        if (Utils.isValidZone((String) actionData.getArray("zones").get(j)))
                        {
                            Zone zone = Zone.getZoneByID((String) actionData.getArray("zones").get(j));

                            JsonObject facilities = new JsonObject();

                            for (Entry<Facility, FacilityInfo> facilityInfo : dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacilities().entrySet())
                            {
                                JsonObject facility = new JsonObject();

                                facility.putString("facility_id", facilityInfo.getKey().getID());
                                facility.putString("facility_type_id", facilityInfo.getKey().getTypeID());
                                facility.putString("owner", facilityInfo.getValue().getOwner().getID());
                                facility.putString("zone_id", zone.getID());

                                facilities.putObject(facilityInfo.getKey().getID(), facility);
                            }

                            zones.putObject(zone.getID(), facilities);
                        }
                    }

                    worldObj.putObject("zones", zones);
                    worlds.putObject(world.getID(), worldObj);
                }
            }

            response.putObject("worlds", worlds);
        }

        else
        {
            response.putString("error", "MissingFilters");
            response.putString("message", "You are missing required filters for this action. Please check your syntax and try again.");
        }

        clientConnection.writeTextFrame(response.encode());
    }
}
