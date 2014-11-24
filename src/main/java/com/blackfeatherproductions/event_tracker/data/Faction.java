package com.blackfeatherproductions.event_tracker.data;

import java.util.List;

public class Faction
{
	private String id;
	private List<String> loadoutIDs;
	private String name;
	private String tag;
	
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
