package com.blackfeatherproductions.event_tracker.data;

import java.util.HashMap;
import java.util.Map;

public class Facility
{
	public static Map<String, Facility> facilities = new HashMap<String, Facility>();
	
	private String id;
	private String name;
	private String type;
	private String typeID;
	
	public Facility(String id, String name, String type, String typeID)
	{
		this.id = id;
		this.name = name;
		this.type = type;
		this.typeID = typeID;
	}

	public String getID()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getType()
	{
		return type;
	}

	public String getTypeID()
	{
		return typeID;
	}

	public static Facility getFacilityByID(String id)
    {
    	return facilities.get(id);
    }
}
