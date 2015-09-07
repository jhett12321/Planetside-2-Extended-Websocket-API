package com.blackfeatherproductions.event_tracker.data_static;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Facility
{
    public static Map<String, Facility> facilities = new HashMap<>();

    private final String id;
    private final String name;
    private final FacilityType type;
    private List<Facility> connectedFacilities = new ArrayList<>();

    public Facility(String id, String name, FacilityType type)
    {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getID()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public FacilityType getType()
    {
        return type;
    }
    
    public void addConnectingFacility(Facility facility)
    {
        if(!connectedFacilities.contains(facility))
        {
            connectedFacilities.add(facility);
        }
    }
    
    public List<Facility> getConnectedFacilities()
    {
        return connectedFacilities;
    }

    public static Facility getFacilityByID(String id)
    {
        return facilities.get(id);
    }
}
