package com.blackfeatherproductions.event_tracker.data;

import java.util.HashMap;
import java.util.Map;

public class World
{
	public static Map<String,World> worlds = new HashMap<String, World>();
	
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
	
    public static World getWorldByID(String id)
    {
    	return worlds.get(id);
    }
    
    public static World getWorldByName(String name)
    {
    	for(World world : worlds.values())
    	{
    		if(world.getName().equalsIgnoreCase(name))
    		{
    			return world;
    		}
    	}
    	
    	return null;
    }
}
