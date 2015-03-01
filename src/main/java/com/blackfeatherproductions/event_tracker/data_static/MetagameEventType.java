package com.blackfeatherproductions.event_tracker.data_static;

import java.util.HashMap;
import java.util.Map;

public class MetagameEventType
{
	public static Map<String, MetagameEventType> metagameEventTypes = new HashMap<String, MetagameEventType>();
	
	private String id;
	private String name;
	private String desc;
	private Zone zone;
	private String facilityID;
	private String facilityTypeID;
	
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
