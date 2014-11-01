package com.blackfeatherproductions.event_tracker.game_data;

import java.util.HashMap;
import java.util.Map;

public class GameData
{
	String[] loadoutsVS = {"15","17","18","19","20","21"};
	String[] loadoutsNC = {"1","3","4","5","6","7"};
	String[] loadoutsTR = {"8","10","11","12","13","14"};
	
	private Map<String, Zone> zones = new HashMap<String, Zone>();
	private Map<String, World> worlds = new HashMap<String, World>();
	//private Map<String, Item> items = new HashMap<String, Item>();
	private Map<String, MetagameEventType> metagameEventTypes = new HashMap<String, MetagameEventType>();
    
    public void UpdateGameData()
    {
        //TODO Query Census for all required static data
    	//TODO Items
    	
    	//Worlds
    	worlds.put("1", new World("1", "Connery"));
    	worlds.put("10", new World("10", "Miller"));
    	worlds.put("13", new World("13", "Cobalt"));
    	worlds.put("17", new World("17", "Emerald"));
    	worlds.put("19", new World("19", "Jaeger"));
    	worlds.put("25", new World("25", "Briggs"));
    	
    	//Zones
    	zones.put("2", new Zone("2", "Indar", "The arid continent of Indar is home to an assortment of biomes. Grassy savannas, rocky canyons, and the open plains of the seabed provide unique challenges to soldiers."));
    	zones.put("4", new Zone("4", "Hossin", "Hossin's dense mangrove and willow forests provide air cover along its many swamps and highlands."));
    	zones.put("6", new Zone("6", "Amerish", "Amerish's lush groves and rocky outcroppings provide ample cover between its rolling plains and mountain passes."));
    	zones.put("8", new Zone("8", "Esamir", "Esamir's expanses of frigid tundra and craggy mountains provide little cover from airborne threats."));
    	
    	//Factions
    	
    	//Metagame Event Types
    	//metagameEventTypes.put("1", new MetagameEventType());
    	//metagameEventTypes.put("2", new MetagameEventType());
    	//metagameEventTypes.put("3", new MetagameEventType());
    	//metagameEventTypes.put("4", new MetagameEventType());
    	//metagameEventTypes.put("51", new MetagameEventType());
    	//metagameEventTypes.put("52", new MetagameEventType());
    	//metagameEventTypes.put("53", new MetagameEventType());
    	//metagameEventTypes.put("54", new MetagameEventType());
    }
    
    public Zone GetZoneByID(String id)
    {
    	return zones.get(id);
    }
    
    public Zone GetZoneByName(String name)
    {
    	for(Zone zone : zones.values())
    	{
    		if(zone.getName().equalsIgnoreCase(name))
    		{
    			return zone;
    		}
    	}
    	
    	return null;
    }
    
    public World GetWorldByID(String id)
    {
    	return worlds.get(id);
    }
    
    public World GetWorldByName(String name)
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
