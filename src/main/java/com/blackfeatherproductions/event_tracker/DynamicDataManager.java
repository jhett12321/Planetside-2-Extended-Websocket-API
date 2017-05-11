package com.blackfeatherproductions.event_tracker;

import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_static.Environment;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.World;

public class DynamicDataManager
{
    // Expired character info objects are reused by new queries.
    private final Queue<CharacterInfo> pooledCharacters = new ConcurrentLinkedQueue<>();

    private final Map<String, CharacterInfo> characters = new ConcurrentHashMap<>();
    private final Map<World, WorldInfo> worlds = new HashMap<>();

    public DynamicDataManager()
    {
        Vertx vertx = EventTracker.instance.getVertx();

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
                        EventTracker.instance.getLogger().warn("Ending MetagameEvent ID " + metagameEvent.getInstanceID() + " on " + worldInfo.getKey().getName() + ": Event is overdue.");

                        JsonObject dummyPayload = new JsonObject();
                        dummyPayload.put("instance_id", metagameEvent.getInstanceID());
                        dummyPayload.put("metagame_event_id", metagameEvent.getType().getID());
                        dummyPayload.put("metagame_event_state", "138");
                        dummyPayload.put("timestamp", String.valueOf(new Date().getTime() / 1000));
                        dummyPayload.put("world_id", worldInfo.getKey().getID());
                        dummyPayload.put("event_name", "MetagameEvent");

                        EventTracker.instance.getEventHandler().handleEvent("MetagameEvent", dummyPayload, Environment.WEBSOCKET_SERVICE);
                    }
                }
            }
        });
        
        //Deletes character data that has reached the expiration period.
        vertx.setPeriodic(600000, id ->
        {
            Iterator<Entry<String, CharacterInfo>> iter = characters.entrySet().iterator();

            //Player cached data expires after 5 minutes.
            while(iter.hasNext())
            {
                Entry<String, CharacterInfo> character = iter.next();

                if(new Date().getTime() - character.getValue().getUpdateTime().getTime() >= 30000)
                {
                    pooledCharacters.add(character.getValue());
                    iter.remove();
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
         CharacterInfo character;

        if (characters.containsKey(characterID))
        {
            character = characters.get(characterID);
        }
        else
        {
            // Check to see if we can reuse an older pooled character.
            if(!pooledCharacters.isEmpty())
            {
                character = pooledCharacters.remove();
            }
            else
            {
                character = new CharacterInfo();
            }
        }

        String characterName = "";
        String factionID = "0";
        String outfitID = "0";
        String zoneID = "0";
        String worldID = "0";
        boolean online = true;

        // We do not have valid data to apply.
        if (characterData != null)
        {
            if (characterData.containsKey("name")) {
                characterName = characterData.getJsonObject("name").getString("first");
            }

            if (characterData.containsKey("faction_id")) {
                factionID = characterData.getString("faction_id");
            }

            if (characterData.containsKey("outfit")) {
                outfitID = characterData.getJsonObject("outfit").getString("outfit_id");
            }

            if (characterData.containsKey("last_event")) {
                zoneID = characterData.getJsonObject("last_event").getString("zone_id");
            }

            if (characterData.containsKey("world")) {
                worldID = characterData.getJsonObject("world").getString("world_id");
            }

            if (characterData.containsKey("online")) {
                online = !characterData.getJsonObject("online").getString("online_status").equals("0");
            }
        }

        character.setCharacterData(characterID, characterName, factionID, outfitID, zoneID, worldID, online);
        characters.put(characterID, character);
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
    
    public void update(String characterID)
    {
        CharacterInfo charInfo = getCharacterData(characterID);

        if(charInfo != null)
        {
            charInfo.updateTime();
        }
    }
}
