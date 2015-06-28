package com.blackfeatherproductions.event_tracker.queries;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.QueryManager;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.ZoneInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

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

        for (int i = 0; i < map_list.size(); i++)
        {
            JsonObject data_zone = data.getJsonArray("map_list").getJsonObject(i);

            Zone zone = Zone.getZoneByID(data_zone.getString("ZoneId"));

            ZoneInfo zoneInfo = worldInfo.getZoneInfo(zone);

            JsonArray data_facilities = data_zone.getJsonObject("Regions").getJsonArray("Row");

            for (int j = 0; j < data_facilities.size(); j++)
            {
                JsonObject data_facility = data_facilities.getJsonObject(j);

                JsonObject data_facility_info = data_facility.getJsonObject("RowData");

                Faction owner = Faction.getFactionByID(data_facility_info.getString("FactionId"));

                JsonObject data_static_facility_info = data_facility_info.getJsonObject("map_region");

                String facility_id = data_static_facility_info.getString("facility_id");
                String facility_name = data_static_facility_info.getString("facility_name");
                String facility_type = data_static_facility_info.getString("facility_type");
                String facility_type_id = data_static_facility_info.getString("facility_type_id");

                Facility facility = Facility.getFacilityByID(facility_id);

                if (facility == null)
                {
                    Facility.facilities.put(facility_id, new Facility(facility_id, facility_name, facility_type, facility_type_id));
                    facility = Facility.getFacilityByID(facility_id);
                }

                zoneInfo.getFacility(facility).setOwner(owner);
            }
        }

        for (Zone zone : Zone.getValidZones())
        {
            JsonObject territoryControl = Utils.calculateTerritoryControl(world, zone);

            dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLocked(true);

            if (territoryControl.getString("control_vs").equals("100"))
            {
                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLockingFaction(Faction.VS);
            }
            else if (territoryControl.getString("control_nc").equals("100"))
            {
                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLockingFaction(Faction.NC);
            }
            else if (territoryControl.getString("control_tr").equals("100"))
            {
                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLockingFaction(Faction.TR);
            }
            else
            {
                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLockingFaction(Faction.NS);
                dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).setLocked(false);
            }
        }

        //Update Metagame Events
        queryManager.queryWorldMetagameEvents(world.getID(), environment);

        worldInfo.setOnline(true);
    }
}
