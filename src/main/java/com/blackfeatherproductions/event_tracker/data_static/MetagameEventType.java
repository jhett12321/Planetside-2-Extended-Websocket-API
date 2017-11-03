package com.blackfeatherproductions.event_tracker.data_static;

import java.util.HashMap;
import java.util.Map;

public class MetagameEventType
{
    public static final Map<String, MetagameEventType> metagameEventTypes = new HashMap<String, MetagameEventType>();

    private final String id;
    private final String name;
    private final String desc;
    private final String categoryID;

    public MetagameEventType(String id, String name, String desc, String categoryID)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.categoryID = categoryID;
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

    public String getCategoryID()
    {
        return categoryID;
    }
    
    public static MetagameEventType getMetagameEventTypeByID(String id)
    {
        return metagameEventTypes.get(id);
    }
}
