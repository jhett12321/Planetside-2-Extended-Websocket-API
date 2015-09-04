package com.blackfeatherproductions.event_tracker.utils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.ZoneInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.FacilityType;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class TerritoryUtils
{
    private static final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();
    
    //TODO Determine if there are no possible paths back to this faction's Warpgate.
    public static void validateBlockingRegions(ZoneInfo zoneInfo)
    {
        
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
        float totalRegions = 0;
        float facilitiesVS = 0;
        float facilitiesNC = 0;
        float facilitiesTR = 0;
        for (Map.Entry<Facility, FacilityInfo> facility : TerritoryUtils.dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacilities().entrySet())
        {
            if (facility.getKey().getType() != FacilityType.WARPGATE && !facility.getValue().isBlocked())
            {
                totalRegions++;
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
        int controlVS = 0;
        int controlNC = 0;
        int controlTR = 0;
        if (totalRegions > 0)
        {
            controlVS = (int) Math.floor(facilitiesVS / totalRegions * 100);
            controlNC = (int) Math.floor(facilitiesNC / totalRegions * 100);
            controlTR = (int) Math.floor(facilitiesTR / totalRegions * 100);
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
        return controlInfo;
    }

    /**
     * Calculates Territory Control for the given world
     *
     * @param world The world ID to calculate territory control.
     *
     * @return A JsonObject containing string values for territory control, and
     * the majority controller (if any).
     */
    public static JsonObject calculateTerritoryControl(World world)
    {
        float totalRegions = 0;
        float facilitiesVS = 0;
        float facilitiesNC = 0;
        float facilitiesTR = 0;
        for (ZoneInfo zone : TerritoryUtils.dynamicDataManager.getWorldInfo(world).getZones().values())
        {
            for (Map.Entry<Facility, FacilityInfo> facility : zone.getFacilities().entrySet())
            {
                if (facility.getKey().getType() != FacilityType.WARPGATE && !facility.getValue().isBlocked())
                {
                    totalRegions++;
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
        if (totalRegions > 0)
        {
            controlVS = (int) Math.floor(facilitiesVS / totalRegions * 100);
            controlNC = (int) Math.floor(facilitiesNC / totalRegions * 100);
            controlTR = (int) Math.floor(facilitiesTR / totalRegions * 100);
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
        return controlInfo;
    }
    
}
