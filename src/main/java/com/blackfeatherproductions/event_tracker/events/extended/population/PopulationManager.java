package com.blackfeatherproductions.event_tracker.events.extended.population;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_dynamic.OnlinePlayer;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class PopulationManager
{
	public Map<String, OnlinePlayer> onlinePlayers = new ConcurrentHashMap<String, OnlinePlayer>();
	
	public PopulationManager()
	{
        EventTracker eventTracker = EventTracker.getInstance();
        Vertx vertx = eventTracker.getVertx();
        
        //Reconnects the websocket if it is not online, or is not responding.
        vertx.setPeriodic(10000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	Iterator<Map.Entry<String, OnlinePlayer>> iter = onlinePlayers.entrySet().iterator();
            	
            	while (iter.hasNext())
            	{
            	    Entry<String, OnlinePlayer> entry = iter.next();
            		
            		if((new Date().getTime() - entry.getValue().getLastEvent().getTime()) / 1000 > 600)
            		{
            			iter.remove();
            		}
            	}
            	
            	generateEvents();
            	
            }
        });
	}
	
	private void generateEvents()
	{
		PopulationStore totalPopulation = new PopulationStore();
		Map<World, PopulationStore> worldPopulations = new HashMap<World, PopulationStore>();
		Map<String, PopulationStore> zonePopulations = new HashMap<String, PopulationStore>();
		
		for(World world : World.getAllWorlds())
		{
			worldPopulations.put(world, new PopulationStore());
			
			for(Zone zone : Zone.getAllZones())
			{
				zonePopulations.put(world.getID() + "_" + zone.getID(), new PopulationStore());
			}
		}
		
		//Count Players
		for(OnlinePlayer player : onlinePlayers.values())
		{
			Faction faction = player.getFaction();
			String outfitID = player.getOutfitID();
			String worldID = player.getWorld().getID();
			String zoneID = player.getZone().getID();
			
			worldPopulations.get(player.getWorld()).incrementPopulation(faction, outfitID);
			zonePopulations.get(worldID + "_" + zoneID).incrementPopulation(faction, outfitID);
			
			totalPopulation.incrementPopulation(faction, outfitID);
		}
		
		//Post Events
		String eventName = "PopulationChange";
		
		//Total Population
		JsonObject totalPayload = new JsonObject();
		totalPayload.putString("population_type", "total");
		totalPayload.putString("population_total", totalPopulation.getTotalPopulation().toString());
		totalPayload.putString("population_vs", totalPopulation.getFactionPopulation(Faction.VS).toString());
		totalPayload.putString("population_nc", totalPopulation.getFactionPopulation(Faction.NC).toString());
		totalPayload.putString("population_tr", totalPopulation.getFactionPopulation(Faction.TR).toString());
		
		EventTracker.getInstance().getEventHandler().handleEvent(eventName, totalPayload);
		
		//World Populations
		for(World world : World.getAllWorlds())
		{
			JsonObject worldPayload = new JsonObject();
			PopulationStore worldPopulation = worldPopulations.get(world);
			
			worldPayload.putString("population_type", "world");
			worldPayload.putString("population_total", worldPopulation.getTotalPopulation().toString());
			worldPayload.putString("population_vs", worldPopulation.getFactionPopulation(Faction.VS).toString());
			worldPayload.putString("population_nc", worldPopulation.getFactionPopulation(Faction.NC).toString());
			worldPayload.putString("population_tr", worldPopulation.getFactionPopulation(Faction.TR).toString());
			worldPayload.putString("world_id", world.getID());
			
			EventTracker.getInstance().getEventHandler().handleEvent(eventName, worldPayload);
			
			for(Entry<String, Integer> outfit : worldPopulation.getOutfitPopulations().entrySet())
			{
				JsonObject outfitPayload = new JsonObject();
				
				outfitPayload.putString("population_type", "outfit");
				outfitPayload.putString("population_total", outfit.getValue().toString());
				outfitPayload.putString("outfit_id", outfit.getKey());
				outfitPayload.putString("world_id", world.getID());
				
				EventTracker.getInstance().getEventHandler().handleEvent(eventName, outfitPayload);
			}
			
			for(Zone zone : Zone.getAllZones())
			{
				JsonObject zonePayload = new JsonObject();
				
				PopulationStore zonePopulation = zonePopulations.get(world.getID() + "_" + zone.getID());
				
				zonePayload.putString("population_type", "zone");
				zonePayload.putString("population_total", zonePopulation.getTotalPopulation().toString());
				zonePayload.putString("population_vs", zonePopulation.getFactionPopulation(Faction.VS).toString());
				zonePayload.putString("population_nc", zonePopulation.getFactionPopulation(Faction.NC).toString());
				zonePayload.putString("population_tr", zonePopulation.getFactionPopulation(Faction.TR).toString());
				zonePayload.putString("zone_id", zone.getID());
				zonePayload.putString("world_id", world.getID());
				
				EventTracker.getInstance().getEventHandler().handleEvent(eventName, zonePayload);
				
				for(Entry<String, Integer> outfit : worldPopulation.getOutfitPopulations().entrySet())
				{
					JsonObject outfitPayload = new JsonObject();
					
					outfitPayload.putString("population_type", "zone_outfit");
					outfitPayload.putString("population_total", outfit.getValue().toString());
					outfitPayload.putString("outfit_id", outfit.getKey());
					outfitPayload.putString("zone_id", zone.getID());
					outfitPayload.putString("world_id", world.getID());
					
					EventTracker.getInstance().getEventHandler().handleEvent(eventName, outfitPayload);
				}
			}
		}
	}
}
