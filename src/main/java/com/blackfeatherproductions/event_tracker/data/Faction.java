package com.blackfeatherproductions.event_tracker.data;

import java.util.List;

public class Faction
{
	public String id;
	public List<String> loadoutIDs;
	public String name;
	public String tag;
	
	public Faction(String id, List<String> loadoutsVS, String name, String tag)
	{
		this.id = id;
		this.loadoutIDs = loadoutsVS;
		this.name = name;
		this.tag = tag;
	}

	public String getId()
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
}
