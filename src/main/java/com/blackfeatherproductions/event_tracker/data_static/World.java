package com.blackfeatherproductions.event_tracker.data_static;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.blackfeatherproductions.event_tracker.EventTracker;

public class World
{
    public static Map<String, World> worlds = new HashMap<String, World>();

    public static World UNKNOWN;

    private final String id;
    private final String name;

    public World(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public String getID()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public static World getWorldByID(String id)
    {
        if (!worlds.containsKey(id))
        {
            EventTracker.getLogger().warn("World ID " + id + "does not exist! Returning unknown world.");
            return World.UNKNOWN;
        }
        else
        {
            return worlds.get(id);
        }
    }

    public static World getWorldByName(String name)
    {
        for (World world : worlds.values())
        {
            if (world.getName().equalsIgnoreCase(name))
            {
                return world;
            }
        }

        return World.UNKNOWN;
    }

    public static Collection<World> getAllWorlds()
    {
        return worlds.values();
    }

    public static Collection<World> getValidWorlds()
    {
        Collection<World> validWorlds = new ArrayList<World>();

        for (World world : worlds.values())
        {
            if (world != World.UNKNOWN)
            {
                validWorlds.add(world);
            }
        }

        return validWorlds;
    }
}
