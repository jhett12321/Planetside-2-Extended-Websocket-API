package com.blackfeatherproductions.event_tracker.server.actions;

import java.util.Map.Entry;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.ZoneInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.utils.CensusUtils;
import com.blackfeatherproductions.event_tracker.utils.TerritoryUtils;

@ActionInfo(actionNames = "worldStatus")
public class WorldStatus implements Action
{
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();

    @Override
    public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
    {
        JsonObject response = new JsonObject();
        response.put("action", "worldStatus");
        
        if (actionData.containsKey("worlds") && actionData.containsKey("zones"))
        {
            response.put("worlds", processWorldZone(actionData));
        }
        
        else if(actionData.containsKey("worlds"))
        {
            response.put("worlds", processWorld(actionData));
        }
        
        else if(actionData.containsKey("zones"))
        {
            response.put("worlds", processZone(actionData));
        }
        
        else
        {
            response.put("worlds", processAll(actionData));
        }

        clientConnection.writeFinalTextFrame(response.encode());
    }
    
    private JsonObject processWorld(JsonObject actionData)
    {
        JsonObject worlds = new JsonObject();

        for (int i = 0; i < actionData.getJsonArray("worlds").size(); i++)
        {
            if (CensusUtils.isValidWorld(actionData.getJsonArray("worlds").getString(i)))
            {
                World world = World.getWorldByID(actionData.getJsonArray("worlds").getString(i));

                JsonObject worldObj = new JsonObject();
                JsonObject zones = new JsonObject();
                
                for (Entry<Zone, ZoneInfo> zone : dynamicDataManager.getWorldInfo(world).getZones().entrySet())
                {
                    JsonObject zoneObj = new JsonObject();

                    //Zone Info
                    String locked = "0";

                    if (zone.getValue().isLocked())
                    {
                        locked = "1";
                    }

                    zoneObj.put("locked", locked);
                    zoneObj.put("locked_by", zone.getValue().getLockingFaction().getID());

                    JsonObject controlInfo = TerritoryUtils.calculateTerritoryControl(world, zone.getKey());

                    zoneObj.put("control_vs", controlInfo.getString("control_vs"));
                    zoneObj.put("control_nc", controlInfo.getString("control_nc"));
                    zoneObj.put("control_tr", controlInfo.getString("control_tr"));
                    
                    //Facility Data
                    JsonArray facilities = new JsonArray();

                    for (Entry<Facility, FacilityInfo> facilityInfo : zone.getValue().getFacilities().entrySet())
                    {
                        JsonObject facility = new JsonObject();

                        facility.put("facility_id", facilityInfo.getKey().getID());
                        facility.put("facility_type_id", facilityInfo.getKey().getType().getID());
                        facility.put("owner", facilityInfo.getValue().getOwner().getID());
                        facility.put("zone_id", zone.getKey().getID());

                        facilities.add(facility);
                    }

                    zoneObj.put("facilities", facilities);

                    //Push this to our main zone list.
                    zones.put(zone.getKey().getID(), zoneObj);
                }

                worldObj.put("zones", zones);
                worlds.put(world.getID(), worldObj);
            }
        }

        return worlds;
    }
    
    private JsonObject processWorldZone(JsonObject actionData)
    {
        JsonObject worlds = new JsonObject();

        for (int i = 0; i < actionData.getJsonArray("worlds").size(); i++)
        {
            if (CensusUtils.isValidWorld(actionData.getJsonArray("worlds").getString(i)))
            {
                World world = World.getWorldByID(actionData.getJsonArray("worlds").getString(i));

                JsonObject worldObj = new JsonObject();
                JsonObject zones = new JsonObject();

                for (int j = 0; j < actionData.getJsonArray("zones").size(); j++)
                {
                    if (CensusUtils.isValidZone(actionData.getJsonArray("zones").getString(j)))
                    {
                        Zone zone = Zone.getZoneByID(actionData.getJsonArray("zones").getString(j));
                        
                        JsonObject zoneObj = new JsonObject();
                        
                        //Zone Info
                        ZoneInfo zoneInfo = dynamicDataManager.getWorldInfo(world).getZoneInfo(zone);
                        
                        String locked = "0";
                        
                        if (zoneInfo.isLocked())
                        {
                            locked = "1";
                        }

                        zoneObj.put("locked", locked);
                        zoneObj.put("locked_by", zoneInfo.getLockingFaction().getID());
                        
                        JsonObject controlInfo = TerritoryUtils.calculateTerritoryControl(world, zone);
                        
                        zoneObj.put("control_vs", controlInfo.getString("control_vs"));
                        zoneObj.put("control_nc", controlInfo.getString("control_nc"));
                        zoneObj.put("control_tr", controlInfo.getString("control_tr"));
                        
                        //Facility Data
                        JsonArray facilities = new JsonArray();

                        for (Entry<Facility, FacilityInfo> facilityInfo : dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacilities().entrySet())
                        {
                            JsonObject facility = new JsonObject();

                            facility.put("facility_id", facilityInfo.getKey().getID());
                            facility.put("facility_type_id", facilityInfo.getKey().getType().getID());
                            facility.put("owner", facilityInfo.getValue().getOwner().getID());
                            facility.put("zone_id", zone.getID());

                            facilities.add(facility);
                        }
                        
                        zoneObj.put("facilities", facilities);

                        //Push this to our main zone list.
                        zones.put(zone.getID(), zoneObj);
                    }
                }

                worldObj.put("zones", zones);
                worlds.put(world.getID(), worldObj);
            }
        }

        return worlds;
    }
    
    private JsonObject processZone(JsonObject actionData)
    {
        JsonObject worlds = new JsonObject();

        for (Entry<World, WorldInfo> world : dynamicDataManager.getAllWorldInfo().entrySet())
        {
            JsonObject worldObj = new JsonObject();
            JsonObject zones = new JsonObject();

            for (int j = 0; j < actionData.getJsonArray("zones").size(); j++)
            {
                if (CensusUtils.isValidZone(actionData.getJsonArray("zones").getString(j)))
                {
                    Zone zone = Zone.getZoneByID(actionData.getJsonArray("zones").getString(j));

                    JsonObject zoneObj = new JsonObject();

                    //Zone Info
                    ZoneInfo zoneInfo = world.getValue().getZoneInfo(zone);

                    String locked = "0";

                    if (zoneInfo.isLocked())
                    {
                        locked = "1";
                    }

                    zoneObj.put("locked", locked);
                    zoneObj.put("locked_by", zoneInfo.getLockingFaction().getID());

                    JsonObject controlInfo = TerritoryUtils.calculateTerritoryControl(world.getKey(), zone);

                    zoneObj.put("control_vs", controlInfo.getString("control_vs"));
                    zoneObj.put("control_nc", controlInfo.getString("control_nc"));
                    zoneObj.put("control_tr", controlInfo.getString("control_tr"));
                    
                    //Facility Data
                    JsonArray facilities = new JsonArray();

                    for (Entry<Facility, FacilityInfo> facilityInfo : world.getValue().getZoneInfo(zone).getFacilities().entrySet())
                    {
                        JsonObject facility = new JsonObject();

                        facility.put("facility_id", facilityInfo.getKey().getID());
                        facility.put("facility_type_id", facilityInfo.getKey().getType().getID());
                        facility.put("owner", facilityInfo.getValue().getOwner().getID());
                        facility.put("zone_id", zone.getID());

                        facilities.add(facility);
                    }

                    zoneObj.put("facilities", facilities);

                    //Push this to our main zone list.
                    zones.put(zone.getID(), zoneObj);
                }
            }

            worldObj.put("zones", zones);
            worlds.put(world.getKey().getID(), worldObj);
        }

        return worlds;
    }
    
    private JsonObject processAll(JsonObject actionData)
    {
        JsonObject worlds = new JsonObject();

        for (Entry<World, WorldInfo> world : dynamicDataManager.getAllWorldInfo().entrySet())
        {
            JsonObject worldObj = new JsonObject();
            JsonObject zones = new JsonObject();

            for (Entry<Zone, ZoneInfo> zone : world.getValue().getZones().entrySet())
            {
                JsonObject zoneObj = new JsonObject();

                //Zone Info
                String locked = "0";

                if (zone.getValue().isLocked())
                {
                    locked = "1";
                }

                zoneObj.put("locked", locked);
                zoneObj.put("locked_by", zone.getValue().getLockingFaction().getID());

                JsonObject controlInfo = TerritoryUtils.calculateTerritoryControl(world.getKey(), zone.getKey());

                zoneObj.put("control_vs", controlInfo.getString("control_vs"));
                zoneObj.put("control_nc", controlInfo.getString("control_nc"));
                zoneObj.put("control_tr", controlInfo.getString("control_tr"));
                
                //Facility Data
                JsonArray facilities = new JsonArray();

                for (Entry<Facility, FacilityInfo> facilityInfo : zone.getValue().getFacilities().entrySet())
                {
                    JsonObject facility = new JsonObject();

                    facility.put("facility_id", facilityInfo.getKey().getID());
                    facility.put("facility_type_id", facilityInfo.getKey().getType().getID());
                    facility.put("owner", facilityInfo.getValue().getOwner().getID());
                    facility.put("zone_id", zone.getKey().getID());

                    facilities.add(facility);
                }

                zoneObj.put("facilities", facilities);

                //Push this to our main zone list.
                zones.put(zone.getKey().getID(), zoneObj);
            }

            worldObj.put("zones", zones);
            worlds.put(world.getKey().getID(), worldObj);
        }

        return worlds;
    }
}
