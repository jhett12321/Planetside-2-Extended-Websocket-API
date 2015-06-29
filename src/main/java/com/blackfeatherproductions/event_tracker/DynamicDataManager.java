package com.blackfeatherproductions.event_tracker;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class DynamicDataManager
{
    private final Map<String, CharacterInfo> characters = new ConcurrentHashMap<String, CharacterInfo>();
    private final Map<World, WorldInfo> worlds = new HashMap<World, WorldInfo>();

    public DynamicDataManager()
    {
        Vertx vertx = EventTracker.getVertx();

        for (World world : World.getValidWorlds())
        {
            worlds.put(world, new WorldInfo(world));
        }

        //Checks that any current metagame events should no-longer be running.
        vertx.setPeriodic(120000, id ->
        {
            for (Entry<World, WorldInfo> worldInfo : worlds.entrySet())
            {
                for (MetagameEventInfo metagameEvent : worldInfo.getValue().getActiveMetagameEvents().values())
                {
                    Date endTime = new Date(Long.parseLong(metagameEvent.getEndTime()) * 1000);

                    if (new Date().after(endTime))
                    {
                        EventTracker.getLogger().warn("Ending MetagameEvent ID " + metagameEvent.getInstanceID() + " on " + worldInfo.getKey().getName() + ": Event is overdue.");

                        JsonObject dummyPayload = new JsonObject();
                        dummyPayload.put("instance_id", metagameEvent.getInstanceID());
                        dummyPayload.put("metagame_event_id", metagameEvent.getType().getID());
                        dummyPayload.put("metagame_event_state", "138");
                        dummyPayload.put("timestamp", String.valueOf(new Date().getTime() / 1000));
                        dummyPayload.put("world_id", worldInfo.getKey().getID());
                        dummyPayload.put("event_name", "MetagameEvent");

                        EventTracker.getEventHandler().handleEvent("MetagameEvent", dummyPayload, Environment.WEBSOCKET_SERVICE);
                    }
                }
            }
        });
    }

    //Character Data
    public boolean characterDataExists(String characterID)
    {
        return characters.containsKey(characterID);
    }

    public CharacterInfo getCharacterData(String characterID)
    {
        return characters.get(characterID);
    }

    public void addCharacterData(String characterID, JsonObject characterData)
    {
        CharacterInfo characterInfo;

        if (characters.containsKey(characterID))
        {
            characterInfo = characters.get(characterID);
        }
        else
        {
            characterInfo = new CharacterInfo(characterID);
        }

        if (characterData != null)
        {
            String characterName = "";
            String factionID = "0";
            String outfitID = "0";
            String zoneID = "0";
            String worldID = "0";
            boolean online = true;

            if (characterData.containsKey("name"))
            {
                characterName = characterData.getJsonObject("name").getString("first");
            }

            if (characterData.containsKey("faction_id"))
            {
                factionID = characterData.getString("faction_id");
            }

            if (characterData.containsKey("outfit"))
            {
                outfitID = characterData.getJsonObject("outfit").getString("outfit_id");
            }

            if (characterData.containsKey("last_event"))
            {
                zoneID = characterData.getJsonObject("last_event").getString("zone_id");
            }

            if (characterData.containsKey("world"))
            {
                worldID = characterData.getJsonObject("world").getString("world_id");
            }

            if (characterData.containsKey("online"))
            {
                online = !characterData.getJsonObject("online").getString("online_status").equals("0");
            }

            characterInfo.setCharacterName(characterName);
            characterInfo.setFaction(Faction.getFactionByID(factionID));
            characterInfo.setOutfitID(outfitID);
            characterInfo.setZone(Zone.getZoneByID(zoneID));
            characterInfo.setWorld(World.getWorldByID(worldID));
            characterInfo.setOnline(online);
            characterInfo.update();
        }

        characters.put(characterID, characterInfo);
    }

    //World Data
    public WorldInfo getWorldInfo(World world)
    {
        if (world != null)
        {
            if (!worlds.containsKey(world))
            {
                worlds.put(world, new WorldInfo(world));
            }

            return worlds.get(world);
        }

        return null;
    }

    public Map<World, WorldInfo> getAllWorldInfo()
    {
        return worlds;
    }

    public void removeCharacter(String characterID)
    {
        characters.remove(characterID);
    }
}
