package com.blackfeatherproductions.event_tracker.queries;

import com.blackfeatherproductions.event_tracker.utils.CensusUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.QueryManager;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.ZoneInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.utils.TerritoryInfo;
import com.blackfeatherproductions.event_tracker.utils.TerritoryUtils;

public class WorldQuery implements Query
{
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();
    private final QueryManager queryManager = EventTracker.getQueryManager();

    private final World world;

    public WorldQuery(String worldID)
    {
        this.world = World.getWorldByID(worldID);
    }

    @Override
    public void receiveData(JsonObject data, Environment environment)
    {
        WorldInfo worldInfo = dynamicDataManager.getWorldInfo(world);

        JsonArray map_list = data.getJsonArray("map_list");

        //Update Facility Data.
        for (int i = 0; i < map_list.size(); i++)
        {
            JsonObject data_zone = data.getJsonArray("map_list").getJsonObject(i);

            Zone zone = Zone.getZoneByID(data_zone.getString("ZoneId"));

            ZoneInfo zoneInfo = worldInfo.getZoneInfo(zone);

            JsonArray regions = data_zone.getJsonObject("Regions").getJsonArray("Row");

            for (int j = 0; j < regions.size(); j++)
            {
                JsonObject region = regions.getJsonObject(j);

                JsonObject region_info = region.getJsonObject("RowData");

                Faction owner = Faction.getFactionByID(region_info.getString("FactionId"));

                JsonObject map_region = region_info.getJsonObject("map_region");

                String facility_id = map_region.getString("facility_id");

                if(CensusUtils.isValidFacility(facility_id))
                {
                    Facility facility = Facility.getFacilityByID(facility_id);

                    zoneInfo.getFacility(facility).setOwner(owner);
                    zoneInfo.getFacility(facility).setBlocked(false);
                }
            }
        }

        for (Zone zone : Zone.getValidZones())
        {
            TerritoryInfo territoryControl = TerritoryUtils.calculateTerritoryControl(world, zone);

            dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLocked(true);

            if (territoryControl.controlVS == 100)
            {
                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLockingFaction(Faction.VS);
            }
            else if (territoryControl.controlNC == 100)
            {
                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLockingFaction(Faction.NC);
            }
            else if (territoryControl.controlTR == 100)
            {
                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLockingFaction(Faction.TR);
            }
            else
            {
                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLockingFaction(Faction.NS);
                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLocked(false);
            }
            
            //Update all zone block statuses.
            TerritoryUtils.updateFacilityBlockedStatus(environment, world, zone, null);
        }

        //Update Metagame Events
        queryManager.queryWorldMetagameEvents(world.getID(), environment);

        worldInfo.setOnline(true);
    }
}
