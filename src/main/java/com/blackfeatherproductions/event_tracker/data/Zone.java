package com.blackfeatherproductions.event_tracker.data;

public class Zone
{
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

}
