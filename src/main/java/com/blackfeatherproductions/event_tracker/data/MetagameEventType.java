package com.blackfeatherproductions.event_tracker.data;

import java.util.HashMap;
import java.util.Map;

public class MetagameEventType
{
	public static Map<String, MetagameEventType> metagameEventTypes = new HashMap<String, MetagameEventType>();
	
	private String id;
	private String name;
	private String desc;
	private String zoneID;
	private String facilityID;
	private String type;
	
	public MetagameEventType(String id, String name, String desc, String zoneID, String facilityID, String type)
	{
		this.id = id;
		this.name = name;
		this.desc = desc;
		this.zoneID = zoneID;
		this.facilityID = facilityID;
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

	public String getDesc()
	{
		return desc;
	}

	public String getZoneID()
	{
		return zoneID;
	}

	public String getFacilityID()
	{
		return facilityID;
	}

	public String getType()
	{
		return type;
	}

    public static MetagameEventType getMetagameEventTypeByID(String id)
    {
    	return metagameEventTypes.get(id);
    }
}
