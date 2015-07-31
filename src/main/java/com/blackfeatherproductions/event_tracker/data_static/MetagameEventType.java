package com.blackfeatherproductions.event_tracker.data_static;

import java.util.HashMap;
import java.util.Map;

public class MetagameEventType
{
    public static final Map<String, MetagameEventType> metagameEventTypes = new HashMap<String, MetagameEventType>();

    private final String id;
    private final String name;
    private final String desc;
    private final Zone zone;
    private final String categoryID;
    private final FacilityType facilityType;

    public MetagameEventType(String id, String name, String desc, Zone zone, String categoryID, FacilityType facilityType)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.zone = zone;
        this.categoryID = categoryID;
        this.facilityType = facilityType;
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

    public Zone getZone()
    {
        return zone;
    }

    public String getCategoryID()
    {
        return categoryID;
    }
    
    public FacilityType getFacilityType()
    {
        return facilityType;
    }
    
    public static MetagameEventType getMetagameEventTypeByID(String id)
    {
        return metagameEventTypes.get(id);
    }
}
