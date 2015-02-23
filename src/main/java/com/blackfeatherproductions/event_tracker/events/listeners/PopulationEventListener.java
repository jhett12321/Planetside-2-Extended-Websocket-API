package com.blackfeatherproductions.event_tracker.events.listeners;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import com.blackfeatherproductions.event_tracker.events.extended.population.PopulationManager;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventName="PopulationEventListener",
listenedEvents = "CharacterList|AchievementEarned|BattleRankUp|Death|DirectiveCompleted|PlayerLogin|PlayerLogout|VehicleDestroy",
priority = EventPriority.NORMAL,
filters = { "no_filtering" })
public class PopulationEventListener implements Event
{
	private DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
	private PopulationManager populationManager = EventTracker.getInstance().getPopulationManager();
	
	private JsonObject payload;

	//Preprocess Info
	private String attackerCharacterID;
	private String characterID;
	
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
			populationManager.onlinePlayers.remove(characterID);
		}
		
		//Vehicle/Combat Events
		if(eventName.equals("Death") || eventName.equals("VehicleDestroy"))
		{
			Faction faction = Faction.getFactionByID(payload.getString("attacker_loadout_id"));
			String outfitID = attacker_character.getOutfitID();
			Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
			World world = World.getWorldByID(payload.getString("world_id"));
			
			if(populationManager.onlinePlayers.containsKey(attackerCharacterID))
			{
				OnlinePlayer player = populationManager.onlinePlayers.get(attackerCharacterID);
				
				player.setLastEvent(new Date());
				player.setFaction(faction);
				player.setOutfitID(outfitID);
				player.setZone(zone);
				player.setWorld(world);
			}
			
			else
			{
				populationManager.onlinePlayers.put(attackerCharacterID, new OnlinePlayer(faction, outfitID, zone, world));
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
		
		World world;
		if(payload.containsField("world_id"))
		{
			world = World.getWorldByID(payload.getString("world_id"));
		}
		else
		{
			world = character.getWorld();
		}
		
		if(populationManager.onlinePlayers.containsKey(characterID))
		{
			OnlinePlayer player = populationManager.onlinePlayers.get(characterID);
			
			player.setLastEvent(new Date());
			player.setFaction(faction);
			player.setOutfitID(outfitID);
			player.setZone(zone);
			player.setWorld(world);
		}
		
		else
		{
			populationManager.onlinePlayers.put(characterID, new OnlinePlayer(faction, outfitID, zone, world));
		}
	}
}
