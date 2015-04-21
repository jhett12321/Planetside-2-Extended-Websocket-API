package com.blackfeatherproductions.event_tracker.data_static;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Zone
{
    public static Map<String, Zone> zones = new HashMap<String, Zone>();

    public static Zone UNKNOWN;
    public static Zone INDAR;
    public static Zone ESAMIR;
    public static Zone AMERISH;
    public static Zone HOSSIN;

    private String id;
    private String name;
    private String desc;

    public Zone(String id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public String getID()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDesc()
    {
        return desc;
    }

    public static Zone getZoneByID(String id)
    {
        return zones.get(id);
    }

    public static Zone getZoneByName(String name)
    {
        for (Zone zone : zones.values())
        {
            if (zone.getName().equalsIgnoreCase(name))
            {
                return zone;
            }
        }

        return null;
    }

    public static Collection<Zone> getAllZones()
    {
        return zones.values();
    }

    public static Collection<Zone> getValidZones()
    {
        Collection<Zone> validZones = new ArrayList<Zone>();

        for (Zone zone : zones.values())
        {
            if (zone != Zone.UNKNOWN)
            {
                validZones.add(zone);
            }
        }

        return validZones;
    }
}
