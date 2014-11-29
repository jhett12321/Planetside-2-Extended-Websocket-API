package com.blackfeatherproductions.event_tracker.data;

public class World
{
	public static World CONNERY;
	public static World MILLER;
	public static World COBALT;
	public static World EMERALD;
	public static World JAEGAR;
	public static World BRIGGS;
	
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
