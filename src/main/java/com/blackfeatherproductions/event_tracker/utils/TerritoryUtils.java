package com.blackfeatherproductions.event_tracker.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.ZoneInfo;
import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.FacilityType;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class TerritoryUtils
{
    private static final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();
    
    /**
     * Validates all facilities in the given zone still contain a connection to a correct Warpgate, updating facilities in the process.
     * 
     * @param environment The environment (PC, PS4) that contains the world/zone requiring facility updates.
     * @param world The world containing the zone that requires updates.
     * @param zone The zone that requires updates.
     * @param timestamp An epoch timestamp (in seconds) to be used for blocked update messages. Specifying null does an internal update without events.
     */
    public static void updateFacilityBlockedStatus(Environment environment, World world, Zone zone, String timestamp)
    {
        ZoneInfo zoneInfo = dynamicDataManager.getWorldInfo(world).getZoneInfo(zone);
        
        for (Entry<Facility, FacilityInfo> regionNode : zoneInfo.getFacilities().entrySet())
        {
            if(regionNode.getKey().getType() != FacilityType.WARPGATE)
            {
                Faction originalOwner = regionNode.getValue().getOwner();

                List<Facility> checkedFacilities = new ArrayList<>();

                boolean blocked = validateRegionNodeBlocked(originalOwner, regionNode.getKey(), zoneInfo, checkedFacilities);
                
                if(blocked != regionNode.getValue().isBlocked())
                {
                    //Update the blocked status of this facility.
                    regionNode.getValue().setBlocked(blocked);
                    
                    //Send an additional FacilityControl event with the updated info.
                    if(timestamp != null)
                    {
                        JsonObject payload = new JsonObject();
                        payload.put("event_name", "FacilityControl");
                        payload.put("timestamp", timestamp);
                        payload.put("world_id", world.getID());
                        payload.put("old_faction_id", regionNode.getValue().getOwner().getID());
                        payload.put("outfit_id", "0");
                        payload.put("new_faction_id", regionNode.getValue().getOwner().getID());
                        payload.put("facility_id", regionNode.getKey().getID());
                        payload.put("zone_id", zone.getID());

                        payload.put("is_blocked_update", "1");

                        String eventName = payload.getString("event_name");

                        EventTracker.getEventHandler().handleEvent(eventName, payload, environment);
                    }
                }
            }
        }
    }
    
    private static boolean validateRegionNodeBlocked(Faction originalOwner, Facility facility, ZoneInfo zoneInfo, List<Facility> checkedFacilities)
    {
        checkedFacilities.add(facility);
        
        for(Facility adjFacility : facility.getConnectedFacilities())
        {
            FacilityInfo adjFacilityInfo = zoneInfo.getFacility(adjFacility);
            
            if(adjFacilityInfo.getOwner() == originalOwner && !checkedFacilities.contains(adjFacility))
            {
                if(adjFacility.getType() == FacilityType.WARPGATE)
                {
                    return false;
                }
                
                else 
                {
                    if(!validateRegionNodeBlocked(originalOwner, adjFacility, zoneInfo, checkedFacilities))
                    {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Calculates Territory Control for the given world + zone
     *
     * @param world The World to calculate territory control for.
     * @param zone The World's zone to calculate territory control for.
     *
     * @return A JsonObject containing string values for territory control, and
     * the majority controller (if any).
     */
    public static JsonObject calculateTerritoryControl(World world, Zone zone)
    {
        float totalFacilities = 0;
        float blockedFacilities = 0;
        
        float facilitiesVS = 0;
        float facilitiesNC = 0;
        float facilitiesTR = 0;
        
        float blockedFacilitiesVS = 0;
        float blockedFacilitiesNC = 0;
        float blockedFacilitiesTR = 0;
        
        Map<Facility, FacilityInfo> facilities = TerritoryUtils.dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacilities();
        
        for (Map.Entry<Facility, FacilityInfo> facility : facilities.entrySet())
        {
            if (facility.getKey().getType() != FacilityType.WARPGATE)
            {
                if(facility.getValue().isBlocked())
                {
                    blockedFacilities++;

                    if (facility.getValue().getOwner() == Faction.VS)
                    {
                        blockedFacilitiesVS++;
                    }

                    else if (facility.getValue().getOwner() == Faction.NC)
                    {
                        blockedFacilitiesNC++;
                    }

                    else if (facility.getValue().getOwner() == Faction.TR)
                    {
                        blockedFacilitiesTR++;
                    }
                }
                
                else
                {
                    totalFacilities++;

                    if (facility.getValue().getOwner() == Faction.VS)
                    {
                        facilitiesVS++;
                    }
                    else if (facility.getValue().getOwner() == Faction.NC)
                    {
                        facilitiesNC++;
                    }
                    else if (facility.getValue().getOwner() == Faction.TR)
                    {
                        facilitiesTR++;
                    }
                }
            }
        }
        
        int controlVS = 0;
        int controlNC = 0;
        int controlTR = 0;
        
        int blockedControlVS = 0;
        int blockedControlNC = 0;
        int blockedControlTR = 0;
        
        if (totalFacilities > 0)
        {
            controlVS = (int) Math.floor(facilitiesVS / totalFacilities * 100);
            controlNC = (int) Math.floor(facilitiesNC / totalFacilities * 100);
            controlTR = (int) Math.floor(facilitiesTR / totalFacilities * 100);
        }
        
        float totalBlockedFacilities = totalFacilities + blockedFacilities;
        
        if(totalBlockedFacilities > 0)
        {
            float totalBlockedFacilitiesVS = facilitiesVS + blockedFacilitiesVS;
            float totalBlockedFacilitiesNC = facilitiesVS + blockedFacilitiesNC;
            float totalBlockedFacilitiesTR = facilitiesVS + blockedFacilitiesTR;
            
            blockedControlVS = (int) Math.floor(totalBlockedFacilitiesVS / totalBlockedFacilities * 100);
            blockedControlNC = (int) Math.floor(totalBlockedFacilitiesNC / totalBlockedFacilities * 100);
            blockedControlTR = (int) Math.floor(totalBlockedFacilitiesTR / totalBlockedFacilities * 100);
        }
        
        int majorityControl = controlVS;
        Faction majorityController = Faction.VS;
        
        if (controlNC > majorityControl)
        {
            majorityControl = controlNC;
            majorityController = Faction.NC;
        }
        
        else if (controlNC == majorityControl)
        {
            majorityController = Faction.NS;
        }
        
        if (controlTR > majorityControl)
        {
            majorityControl = controlTR;
            majorityController = Faction.TR;
        }
        
        else if (controlTR == majorityControl)
        {
            majorityController = Faction.NS;
        }
        
        JsonObject controlInfo = new JsonObject();
        
        controlInfo.put("control_vs", String.valueOf(controlVS));
        controlInfo.put("control_nc", String.valueOf(controlNC));
        controlInfo.put("control_tr", String.valueOf(controlTR));
        controlInfo.put("majority_controller", majorityController.getID());
        controlInfo.put("total_vs", String.valueOf(blockedControlVS));
        controlInfo.put("total_nc", String.valueOf(blockedControlNC));
        controlInfo.put("total_tr", String.valueOf(blockedControlTR));
        
        return controlInfo;
    }
}
