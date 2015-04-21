package com.blackfeatherproductions.event_tracker.data_static;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Faction
{
    public static Map<String, Faction> factions = new HashMap<String, Faction>();

    public static Faction NS;
    public static Faction VS;
    public static Faction NC;
    public static Faction TR;

    private String id;
    private List<String> loadoutIDs;
    private String name;
    private String tag;

    public Faction(String id, List<String> loadoutsVS, String name, String tag)
    {
        this.id = id;
        this.loadoutIDs = loadoutsVS;
        this.name = name;
        this.tag = tag;
    }

    public String getID()
    {
        return id;
    }

    public List<String> getLoadoutIDs()
    {
        return loadoutIDs;
    }

    public String getName()
    {
        return name;
    }

    public String getTag()
    {
        return tag;
    }

    public static Faction getFactionByID(String id)
    {
        return factions.get(id);
    }

    public static Faction getFactionByLoadoutID(String loadoutID)
    {
        for (Faction faction : factions.values())
        {
            if (faction.getLoadoutIDs().contains(loadoutID))
            {
                return faction;
            }
        }
        return null;
    }
}
