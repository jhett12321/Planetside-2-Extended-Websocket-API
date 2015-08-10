package com.blackfeatherproductions.event_tracker.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class CensusUtils
{

    public static List<String> getWorldIDsFromEndpointString(String endPointString)
    {
        List<String> worldIDs = new ArrayList<>();
        Pattern p = Pattern.compile("[0-9]+");
        Matcher m = p.matcher(endPointString);
        while (m.find())
        {
            String worldID = m.group();
            worldIDs.add(worldID);
        }
        return worldIDs;
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
    
}
