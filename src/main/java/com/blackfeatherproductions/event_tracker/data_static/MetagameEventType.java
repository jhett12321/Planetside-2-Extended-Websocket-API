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
    private final String facilityID;
    private final String facilityTypeID;

    public MetagameEventType(String id, String name, String desc, Zone zone, String facilityID, String facilityTypeID)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.zone = zone;
        this.facilityID = facilityID;
        this.facilityTypeID = facilityTypeID;
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

    public String getFacilityID()
    {
        return facilityID;
    }

    public String getFacilityTypeID()
    {
        return facilityTypeID;
    }

    public static MetagameEventType getMetagameEventTypeByID(String id)
    {
        return metagameEventTypes.get(id);
    }
}
