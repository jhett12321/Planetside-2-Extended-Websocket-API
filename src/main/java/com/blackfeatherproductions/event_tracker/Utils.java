package com.blackfeatherproductions.event_tracker;

import java.util.Map.Entry;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.ZoneInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class Utils
{
    private static DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();

    public static String getWorldIDFromEndpointString(String endPointString)
    {
        String worldID = endPointString.replaceAll("(\\D+)", "");

        return worldID;
    }

    public static boolean isValidPayload(JsonObject payload)
    {
        if (payload.containsField("zone_id"))
        {
            if (!isValidZone(payload.getString("zone_id")))
            {
                return false;
            }
        }

        if (payload.containsField("attacker_character_id"))
        {
            if (!isValidCharacter(payload.getString("attacker_character_id")) && !isValidCharacter(payload.getString("character_id")))
            {
                return false;
            }
        }

        else if (payload.containsField("character_id"))
        {
            if (!isValidCharacter(payload.getString("character_id")))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the given Zone ID is valid.
     *
     * @param zoneID The zone ID whose validity needs to be checked.
     * @return true if this Zone ID is valid.
     */
    public static boolean isValidZone(String zoneID)
    {
        if (Zone.zones.containsKey(zoneID))
        {
            return true;
        }

        return false;
    }

    /**
     * Checks if the given World ID is valid.
     *
     * @param worldID The world ID whose validity needs to be checked.
     * @return true if this World ID is valid.
     */
    public static boolean isValidWorld(String worldID)
    {
        if (World.worlds.containsKey(worldID))
        {
            return true;
        }

        return false;
    }

    /**
     * Checks if the given Character ID is valid.
     *
     * @param characterID The character ID whose validity needs to be checked.
     * @return true if this character ID is valid.
     */
    public static boolean isValidCharacter(String characterID)
    {
        if (characterID != null && characterID.length() == 19)
        {
            return true;
        }

        return false;
    }

    /**
     * Calculates Territory Control for the given world
     *
     * @param world The World to calculate territory control for.
     * @param zone The World's zone to calculate territory control for.
     * @return A JsonObject containing string values for territory control, and
     * the majority controller (if any).
     */
    public static JsonObject calculateTerritoryControl(World world, Zone zone)
    {
        float totalRegions = 0;
        float facilitiesVS = 0;
        float facilitiesNC = 0;
        float facilitiesTR = 0;

        for (Entry<Facility, FacilityInfo> facility : dynamicDataManager.getWorldInfo(world).getZoneInfo(zone).getFacilities().entrySet())
        {
            //Territory Control ignores warpgates.
            if (!facility.getKey().getTypeID().equals("7"))
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

        Integer controlVS = 0;
        Integer controlNC = 0;
        Integer controlTR = 0;

        if (totalRegions > 0)
        {
            controlVS = (int) Math.floor(facilitiesVS / totalRegions * 100);
            controlNC = (int) Math.floor(facilitiesNC / totalRegions * 100);
            controlTR = (int) Math.floor(facilitiesTR / totalRegions * 100);
        }

        float majorityControl = controlVS;
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

        controlInfo.putString("control_vs", controlVS.toString());
        controlInfo.putString("control_nc", controlNC.toString());
        controlInfo.putString("control_tr", controlTR.toString());
        controlInfo.putString("majority_controller", majorityController.getID());

        return controlInfo;
    }

    /**
     * Calculates Territory Control for the given world
     *
     * @param world The world ID to calculate territory control.
     * @return A JsonObject containing string values for territory control, and
     * the majority controller (if any).
     */
    public static JsonObject calculateTerritoryControl(World world)
    {
        float totalRegions = 0;
        float facilitiesVS = 0;
        float facilitiesNC = 0;
        float facilitiesTR = 0;

        for (ZoneInfo zone : dynamicDataManager.getWorldInfo(world).getZones().values())
        {
            for (FacilityInfo facility : zone.getFacilities().values())
            {
                totalRegions++;

                if (facility.getOwner() == Faction.VS)
                {
                    facilitiesVS++;
                }
                else if (facility.getOwner() == Faction.NC)
                {
                    facilitiesNC++;
                }
                else if (facility.getOwner() == Faction.TR)
                {
                    facilitiesTR++;
                }
            }
        }

        Float controlVS = 0f;
        Float controlNC = 0f;
        Float controlTR = 0f;

        if (totalRegions > 0)
        {
            controlVS = (float) Math.floor(facilitiesVS / totalRegions * 100);
            controlNC = (float) Math.floor(facilitiesNC / totalRegions * 100);
            controlTR = (float) Math.floor(facilitiesTR / totalRegions * 100);
        }

        float majorityControl = controlVS;
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

        controlInfo.putString("control_vs", controlVS.toString());
        controlInfo.putString("control_nc", controlNC.toString());
        controlInfo.putString("control_tr", controlTR.toString());
        controlInfo.putString("majority_controller", majorityController.getID());

        return controlInfo;
    }
}
