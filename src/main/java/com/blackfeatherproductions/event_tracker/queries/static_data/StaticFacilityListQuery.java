package com.blackfeatherproductions.event_tracker.queries.static_data;

import com.blackfeatherproductions.event_tracker.queries.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.QueryManager;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.FacilityType;

public class StaticFacilityListQuery implements Query
{
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();
    private final QueryManager queryManager = EventTracker.getQueryManager();

    @Override
    public void receiveData(JsonObject data, Environment environment)
    {
        JsonArray map_region_list = data.getJsonArray("map_region_list");
        
        //Create Static Facility Instances if they do not exist.
        for (int i = 0; i < map_region_list.size(); i++)
        {
            JsonObject map_region = map_region_list.getJsonObject(i);
            
            String facility_id = map_region.getString("facility_id");
            String facility_name = map_region.getString("facility_name");
            String facility_type_id = map_region.getString("facility_type_id");

            Facility facility = Facility.getFacilityByID(facility_id);
            FacilityType facility_type = FacilityType.getFacilityTypeByID(facility_type_id);

            if (facility == null)
            {
                Facility.facilities.put(facility_id, new Facility(facility_id, facility_name, facility_type));
            }
        }
        
        //Update Static Facility Connection/Lattice Link
        for (int i = 0; i < map_region_list.size(); i++)
        {
            JsonObject map_region = map_region_list.getJsonObject(i);
            
            String facility_id = map_region.getString("facility_id");
            String facility_name = map_region.getString("facility_name");
            String facility_type_id = map_region.getString("facility_type_id");

            Facility facility = Facility.getFacilityByID(facility_id);
            FacilityType facility_type = FacilityType.getFacilityTypeByID(facility_type_id);
            
            JsonArray connecting_links = map_region.getJsonArray("connecting_links");
           
            for (int j = 0; j < connecting_links.size(); j++)
            {
                Facility connectingFacility = Facility.getFacilityByID(connecting_links.getJsonObject(j).getString("facility_id_b"));
                
                if(connectingFacility != null)
                {
                    facility.addConnectingFacility(connectingFacility);
                }
            }
        }
    }
}
