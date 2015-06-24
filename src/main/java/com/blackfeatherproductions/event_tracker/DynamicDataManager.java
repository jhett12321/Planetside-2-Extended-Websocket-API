package com.blackfeatherproductions.event_tracker;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.queries.Environment;

public class DynamicDataManager
{
    private final EventTracker eventTracker = EventTracker.getInstance();

    private final Map<String, CharacterInfo> characters = new ConcurrentHashMap<String, CharacterInfo>();
    private final Map<World, WorldInfo> worlds = new HashMap<World, WorldInfo>();

    public DynamicDataManager()
    {
        Vertx vertx = eventTracker.getVertx();

        for (World world : World.getValidWorlds())
        {
            worlds.put(world, new WorldInfo(world));
        }

        //Checks that any current metagame events should no-longer be running.
        vertx.setPeriodic(120000, new Handler<Long>()
        {
            @Override
            public void handle(Long timerID)
            {
                for (Entry<World, WorldInfo> worldInfo : worlds.entrySet())
                {
                    for (MetagameEventInfo metagameEvent : worldInfo.getValue().getActiveMetagameEvents().values())
                    {
                        Date endTime = new Date(Long.parseLong(metagameEvent.getEndTime()) * 1000);

                        if (new Date().after(endTime))
                        {
                            eventTracker.getLogger().warn("Ending MetagameEvent ID " + metagameEvent.getInstanceID() + " on " + worldInfo.getKey().getName() + ": Event is overdue.");

                            JsonObject dummyPayload = new JsonObject();
                            dummyPayload.putString("instance_id", metagameEvent.getInstanceID());
                            dummyPayload.putString("metagame_event_id", metagameEvent.getType().getID());
                            dummyPayload.putString("metagame_event_state", "138");
                            dummyPayload.putString("timestamp", String.valueOf(new Date().getTime() / 1000));
                            dummyPayload.putString("world_id", worldInfo.getKey().getID());
                            dummyPayload.putString("event_name", "MetagameEvent");

                            eventTracker.getEventHandler().handleEvent("MetagameEvent", dummyPayload, Environment.WEBSOCKET_SERVICE);
                        }
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

    public void addCharacterData(String characterID, CharacterInfo character)
    {
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

    public void removeCharacter(String characterID)
    {
        characters.remove(characterID);
    }
}
