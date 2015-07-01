package com.blackfeatherproductions.event_tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.ZoneInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class Utils
{
    private static final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();

    public static List<String> getWorldIDsFromEndpointString(String endPointString)
    {
        List<String> worldIDs = new ArrayList<>();

        //Match all ID values we get from the endpoint message.
        Pattern p = Pattern.compile("[0-9]+");
        Matcher m = p.matcher(endPointString);

        while (m.find())
        {
            String worldID = m.group();
            worldIDs.add(worldID);
        }

        return worldIDs;
    }

    public static boolean isValidPayload(JsonObject payload)
    {
        if (payload.containsKey("zone_id"))
        {
            if (!isValidZone(payload.getString("zone_id")))
            {
                return false;
            }
        }

        if (payload.containsKey("attacker_character_id"))
        {
            if (!isValidCharacter(payload.getString("attacker_character_id")) && !isValidCharacter(payload.getString("character_id")))
            {
                return false;
            }
        }

        else if (payload.containsKey("character_id"))
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
     *
     * @return true if this Zone ID is valid.
     */
    public static boolean isValidZone(String zoneID)
    {
        return Zone.zones.containsKey(zoneID);
    }

    /**
     * Checks if the given World ID is valid.
     *
     * @param worldID The world ID whose validity needs to be checked.
     *
     * @return true if this World ID is valid.
     */
    public static boolean isValidWorld(String worldID)
    {
        return World.worlds.containsKey(worldID);
    }

    /**
     * Checks if the given Character ID is valid.
     *
     * @param characterID The character ID whose validity needs to be checked.
     *
     * @return true if this character ID is valid.
     */
    public static boolean isValidCharacter(String characterID)
    {
        return characterID != null && characterID.length() == 19;
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
        
        for (ZoneInfo zone : dynamicDataManager.getWorldInfo(world).getZones().values())
        {
            for (Entry<Facility, FacilityInfo> facility : zone.getFacilities().entrySet())
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
