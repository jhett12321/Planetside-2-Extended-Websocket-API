package com.blackfeatherproductions.event_tracker.events.listeners;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.OnlinePlayer;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.extended.population.PopulationStore;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventName="PopulationEventListener",
listenedEvents = "AchievementEarned|BattleRankUp|Death|DirectiveCompleted|PlayerLogin|PlayerLogout|VehicleDestroy",
priority = EventPriority.LISTENER,
filters = { "no_filtering" })
public class PopulationEventListener implements Event
{
	private Map<String, OnlinePlayer> onlinePlayers = new ConcurrentHashMap<String, OnlinePlayer>();
	
	private DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
	
	private JsonObject payload;

	//Preprocess Info
	private String attackerCharacterID;
	private String characterID;
	
	public PopulationEventListener()
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
            	
            	GenerateEvents();
            	
            }
        });
	}
	
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		this.payload = payload;
		
		if(payload != null)
		{
			attackerCharacterID = payload.getString("attacker_character_id");
			characterID = payload.getString("character_id");
			
			if(!Utils.isValidCharacter(characterID))
			{
				characterID = attackerCharacterID;
			}
			
			if(!Utils.isValidCharacter(attackerCharacterID))
			{
				attackerCharacterID = characterID;
			}
			
			if(dynamicDataManager.characterDataExists(attackerCharacterID) && dynamicDataManager.characterDataExists(characterID))
			{
				processEvent();
			}
			
			else
			{
				List<String> characterIDs = new ArrayList<String>();
				characterIDs.add(characterID);
				
				if(attackerCharacterID != characterID)
				{
					characterIDs.add(attackerCharacterID);
				}
				
				new CharacterQuery(characterIDs, this);
			}
		}
	}

	@Override
	public void processEvent()
	{
		CharacterInfo attacker_character = dynamicDataManager.getCharacterData(attackerCharacterID);
		CharacterInfo character = dynamicDataManager.getCharacterData(characterID);
		
		String eventName = payload.getString("event_name");
		
		//Logout Event.
		if(eventName.equals("PlayerLogout"))
		{
			onlinePlayers.remove(characterID);
		}
		
		//Vehicle/Combat Events
		if(attackerCharacterID != characterID)
		{
			Faction faction = Faction.getFactionByID(payload.getString("attacker_loadout_id"));
			String outfitID = attacker_character.getOutfitID();
			Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
			World world = World.getWorldByID(payload.getString("world_id"));
			
			if(onlinePlayers.containsKey(attackerCharacterID))
			{
				OnlinePlayer player = onlinePlayers.get(attackerCharacterID);
				
				player.setLastEvent(new Date());
				player.setFaction(faction);
				player.setOutfitID(outfitID);
				player.setZone(zone);
				player.setWorld(world);
			}
			
			else
			{
				onlinePlayers.put(attackerCharacterID, new OnlinePlayer(faction, outfitID, zone, world));
			}
		}
		
		//All Character Events
		Faction faction;
		
		if(payload.containsField("loadout_id"))
		{
			faction = Faction.getFactionByID(payload.getString("loadout_id"));
		}
		
		else if(payload.containsField("faction_id"))
		{
			faction = Faction.getFactionByID(payload.getString("faction_id"));
		}
		
		else
		{
			faction = character.getFaction();
		}
		
		String outfitID = character.getOutfitID();
		Zone zone;
		
		if(payload.containsField("zone_id"))
		{
			zone = Zone.getZoneByID(payload.getString("zone_id"));
		}
		
		else
		{
			zone = character.getZone();
		}
		
		if(zone == null)
		{
			zone = Zone.UNKNOWN;
		}
		
		World world = World.getWorldByID(payload.getString("world_id"));
		
		if(onlinePlayers.containsKey(characterID))
		{
			OnlinePlayer player = onlinePlayers.get(characterID);
			
			player.setLastEvent(new Date());
			player.setFaction(faction);
			player.setOutfitID(outfitID);
			player.setZone(zone);
			player.setWorld(world);
		}
		
		else
		{
			onlinePlayers.put(characterID, new OnlinePlayer(faction, outfitID, zone, world));
		}
	}
	
	private void GenerateEvents()
	{
		PopulationStore totalPopulation = new PopulationStore();
		Map<World, PopulationStore> worldPopulations = new HashMap<World, PopulationStore>();
		Map<String, PopulationStore> zonePopulations = new HashMap<String, PopulationStore>();
		
		for(World world : World.worlds.values())
		{
			worldPopulations.put(world, new PopulationStore());
			
			for(Zone zone : Zone.zones.values())
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
		totalPayload.putString("population_total", totalPopulation.getTotalPopulation().toString());
		totalPayload.putString("population_vs", totalPopulation.getFactionPopulation(Faction.VS).toString());
		totalPayload.putString("population_nc", totalPopulation.getFactionPopulation(Faction.NC).toString());
		totalPayload.putString("population_tr", totalPopulation.getFactionPopulation(Faction.TR).toString());
		totalPayload.putString("outfit_id", "-1");
		totalPayload.putString("zone_id", "-1");
		totalPayload.putString("world_id", "-1");
		
		EventTracker.getInstance().getEventHandler().handleEvent(eventName, totalPayload);
		
		//World Populations
		for(World world : World.worlds.values())
		{
			JsonObject worldPayload = new JsonObject();
			PopulationStore worldPopulation = worldPopulations.get(world);
			
			worldPayload.putString("population_total", worldPopulation.getTotalPopulation().toString());
			worldPayload.putString("population_vs", worldPopulation.getFactionPopulation(Faction.VS).toString());
			worldPayload.putString("population_nc", worldPopulation.getFactionPopulation(Faction.NC).toString());
			worldPayload.putString("population_tr", worldPopulation.getFactionPopulation(Faction.TR).toString());
			worldPayload.putString("outfit_id", "-1");
			worldPayload.putString("zone_id", "-1");
			worldPayload.putString("world_id", world.getID());
			
			EventTracker.getInstance().getEventHandler().handleEvent(eventName, worldPayload);
			
			for(Entry<String, Integer> outfit : worldPopulation.getOutfitPopulations().entrySet())
			{
				JsonObject outfitPayload = new JsonObject();
				
				outfitPayload.putString("population_total", outfit.getValue().toString());
				outfitPayload.putString("population_vs", "-1");
				outfitPayload.putString("population_nc", "-1");
				outfitPayload.putString("population_tr", "-1");
				outfitPayload.putString("outfit_id", outfit.getKey());
				outfitPayload.putString("zone_id", "-1");
				outfitPayload.putString("world_id", world.getID());
				
				EventTracker.getInstance().getEventHandler().handleEvent(eventName, outfitPayload);
			}
			
			for(Zone zone : Zone.zones.values())
			{
				JsonObject zonePayload = new JsonObject();
				
				PopulationStore zonePopulation = zonePopulations.get(world.getID() + "_" + zone.getID());
				
				zonePayload.putString("population_total", zonePopulation.getTotalPopulation().toString());
				zonePayload.putString("population_vs", zonePopulation.getFactionPopulation(Faction.VS).toString());
				zonePayload.putString("population_nc", zonePopulation.getFactionPopulation(Faction.NC).toString());
				zonePayload.putString("population_tr", zonePopulation.getFactionPopulation(Faction.TR).toString());
				zonePayload.putString("outfit_id", "-1");
				zonePayload.putString("zone_id", zone.getID());
				zonePayload.putString("world_id", world.getID());
				
				EventTracker.getInstance().getEventHandler().handleEvent(eventName, zonePayload);
				
				for(Entry<String, Integer> outfit : worldPopulation.getOutfitPopulations().entrySet())
				{
					JsonObject outfitPayload = new JsonObject();
					
					outfitPayload.putString("population_total", outfit.getValue().toString());
					outfitPayload.putString("population_vs", "-1");
					outfitPayload.putString("population_nc", "-1");
					outfitPayload.putString("population_tr", "-1");
					outfitPayload.putString("outfit_id", outfit.getKey());
					outfitPayload.putString("zone_id", zone.getID());
					outfitPayload.putString("world_id", world.getID());
					
					EventTracker.getInstance().getEventHandler().handleEvent(eventName, outfitPayload);
				}
			}
		}
	}
}
