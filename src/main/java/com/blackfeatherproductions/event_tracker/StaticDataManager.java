package com.blackfeatherproductions.event_tracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.MetagameEventType;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;

public class StaticDataManager
{
	//Static Data
	private Map<String, World> worlds;
	private Map<String, Zone> zones;
	
	private Map<String, Faction> factions;
	//private Map<String, Item> items = new HashMap<String, Item>();
	
	private Map<String, MetagameEventType> metagameEventTypes;
	
	public StaticDataManager()
	{   
        //Initialises Static Game Data
		
        //TODO Query Census for all required static data
		//TODO Each Data Object needs to have a "GetFormattedMessage" function
		//TODO All Data Types need to eventually have their own class.
    	//TODO Types to be implemented: achievement, item/weapon, vehicle, directive, facility
    	worlds = new HashMap<String, World>();
    	zones = new HashMap<String, Zone>();
    	
    	factions = new HashMap<String, Faction>();
    	
    	metagameEventTypes = new HashMap<String, MetagameEventType>();
    	
    	//Worlds
    	World.CONNERY = new World("1", "Connery");
    	World.MILLER = new World("10", "Miller");
    	World.COBALT = new World("13", "Cobalt");
    	World.EMERALD = new World("17", "Emerald");
    	World.JAEGAR = new World("19", "Jaeger");
    	World.BRIGGS = new World("25", "Briggs");
    	
    	worlds.put("1", World.CONNERY);
    	worlds.put("10", World.MILLER);
    	worlds.put("13", World.COBALT);
    	worlds.put("17", World.EMERALD);
    	worlds.put("19", World.JAEGAR);
    	worlds.put("25", World.BRIGGS);
    	
    	//Zones
    	Zone.INDAR = new Zone("2", "Indar", "The arid continent of Indar is home to an assortment of biomes. Grassy savannas, rocky canyons, and the open plains of the seabed provide unique challenges to soldiers.");
    	Zone.HOSSIN = new Zone("4", "Hossin", "Hossin's dense mangrove and willow forests provide air cover along its many swamps and highlands.");
    	Zone.AMERISH = new Zone("6", "Amerish", "Amerish's lush groves and rocky outcroppings provide ample cover between its rolling plains and mountain passes.");
    	Zone.ESAMIR = new Zone("8", "Esamir", "Esamir's expanses of frigid tundra and craggy mountains provide little cover from airborne threats.");
    	
    	zones.put("2", Zone.INDAR );
    	zones.put("4", Zone.HOSSIN);
    	zones.put("6", Zone.AMERISH);
    	zones.put("8", Zone.ESAMIR);
    	
    	//Factions
    	List<String> loadoutsVS = new ArrayList<String>();
    	loadoutsVS.add("15");
    	loadoutsVS.add("17");
    	loadoutsVS.add("18");
    	loadoutsVS.add("19");
    	loadoutsVS.add("20");
    	loadoutsVS.add("21");
    	
    	List<String> loadoutsNC = new ArrayList<String>();
    	loadoutsNC.add("1");
    	loadoutsNC.add("3");
    	loadoutsNC.add("4");
    	loadoutsNC.add("5");
    	loadoutsNC.add("6");
    	loadoutsNC.add("7");
    	
    	List<String> loadoutsTR = new ArrayList<String>();
    	loadoutsTR.add("8");
    	loadoutsTR.add("10");
    	loadoutsTR.add("11");
    	loadoutsTR.add("12");
    	loadoutsTR.add("13");
    	loadoutsTR.add("14");
    	
    	Faction.VS = new Faction("1", loadoutsVS, "Vanu Sovereignty", "VS");
    	Faction.NC = new Faction("2", loadoutsNC, "New Conglomerate", "NC");
    	Faction.TR = new Faction("3", loadoutsTR, "Terran Republic", "TR");
    	
    	factions.put("1", Faction.VS);
    	factions.put("2", Faction.NC);
    	factions.put("3", Faction.TR);
    	
    	//Metagame Event Types
    	metagameEventTypes.put("1", new MetagameEventType("1", "Feeling the Heat", "Capture Indar within the time limit", "2", "0", "1"));
    	metagameEventTypes.put("2", new MetagameEventType("2", "Cold War", "Capture Esamir within the time limit", "8", "0", "1"));
    	metagameEventTypes.put("3", new MetagameEventType("3", "Seeing Green", "Capture Amerish within the time limit", "6", "0", "1"));
    	metagameEventTypes.put("4", new MetagameEventType("4", "Marsh Madness", "Capture Hossin within the time limit", "4", "0", "1"));
    	metagameEventTypes.put("4", new MetagameEventType("51", "Indar Pumpkin Hunt", "Seek and destroy pumpkins on Indar", "2", "0", "5"));
    	metagameEventTypes.put("4", new MetagameEventType("52", "Esamir Pumpkin Hunt", "Seek and destroy pumpkins on Esamir", "8", "0", "5"));
    	metagameEventTypes.put("4", new MetagameEventType("53", "Amerish Pumpkin Hunt", "Seek and destroy pumpkins on Amerish", "6", "0", "5"));
    	metagameEventTypes.put("4", new MetagameEventType("54", "Hossin Pumpkin Hunt", "Seek and destroy pumpkins on Hossin", "4", "0", "5"));
	}
    
    //Worlds
    public World getWorldByID(String id)
    {
    	return worlds.get(id);
    }
    
    public World getWorldByName(String name)
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
    
    //Zones
    public Zone getZoneByID(String id)
    {
    	return zones.get(id);
    }
    
    public Zone getZoneByName(String name)
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
    
    //Factions
    public Faction getFactionByID(String id)
    {
    	return factions.get(id);
    }
    
    public Faction getFactionByLoadoutID(String loadoutID)
    {
    	for(Faction faction : factions.values())
    	{
    		if(faction.getLoadoutIDs().contains(loadoutID))
    		{
    			return faction;
    		}
    	}
    	return null;
    }
    
    //MetagameEvent Types
    public MetagameEventType getMetagameEventTypeByID(String id)
    {
    	return metagameEventTypes.get(id);
    }
}
