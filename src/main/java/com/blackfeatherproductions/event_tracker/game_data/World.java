package com.blackfeatherproductions.event_tracker.game_data;

public class World
{
	private String id;
	private String name;
	
	public World(String id, String name)
	{
		this.id = id;
		this.name = name;
	}
	
	public String getID()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}
}
