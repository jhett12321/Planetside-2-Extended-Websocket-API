package com.blackfeatherproductions.event_tracker.events;

import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;
import com.blackfeatherproductions.event_tracker.data.dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventNames = "VehicleDestroy", priority = EventPriority.NORMAL)
public class VehicleDestroyEvent implements Event
{
	private DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
	
	private JsonObject payload;
	
	//Preprocess Info
	private String attackerCharacterID;
	private String victimCharacterID;

	@Override
	public void preProcessEvent(JsonObject payload)
	{
		this.payload = payload;
		if(payload != null)
		{
			attackerCharacterID = payload.getString("attacker_character_id");
			victimCharacterID = payload.getString("character_id");
			
			if(!Utils.isValidCharacter(victimCharacterID))
			{
				victimCharacterID = attackerCharacterID;
			}
			
			if(!Utils.isValidCharacter(attackerCharacterID))
			{
				attackerCharacterID = victimCharacterID;
			}
			
			if(dynamicDataManager.characterDataExists(attackerCharacterID) && dynamicDataManager.characterDataExists(victimCharacterID))
			{
				processEvent();
			}
			
			else
			{
				List<String> characterIDs = new ArrayList<String>();
				characterIDs.add(attackerCharacterID);
				characterIDs.add(victimCharacterID);
				
				new CharacterQuery(characterIDs, this);
			}
		}
	}

	@Override
	public void processEvent()
	{
		//Data
		CharacterInfo attacker_character = dynamicDataManager.getCharacterData(attackerCharacterID);
		String attacker_outfit_id = attacker_character.getOutfitID();
		String attacker_loadout_id = payload.getString("attacker_loadout_id");
		Faction attacker_faction = Faction.getFactionByLoadoutID(attacker_loadout_id);
		String attacker_vehicle_id = payload.getString("attacker_vehicle_id");
		
		CharacterInfo victim_character = dynamicDataManager.getCharacterData(victimCharacterID);
		String victim_outfit_id = victim_character.getOutfitID();
		Faction victim_faction = Faction.getFactionByID(payload.getString("faction_id"));
		String victim_vehicle_id = payload.getString("vehicle_id");
		
		String weapon_id = payload.getString("attacker_weapon_id");
		
		String timestamp = payload.getString("timestamp");
		String facility_id = payload.getString("facility_id");
		Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
		World world = World.getWorldByID(payload.getString("world_id"));
		
		//Payload
		JsonObject eventData = new JsonObject();
		
		eventData.putString("attacker_character_id", attacker_character.getCharacterID());
		eventData.putString("attacker_outfit_id", attacker_outfit_id);
		eventData.putString("attacker_loadout_id", attacker_loadout_id);
		eventData.putString("attacker_faction_id", attacker_faction.getId());
		eventData.putString("attacker_vehicle_id", attacker_vehicle_id);
		
		eventData.putString("victim_character_id", victim_character.getCharacterID());
		eventData.putString("victim_outfit_id", victim_outfit_id);
		eventData.putString("victim_faction_id", victim_faction.getId());
		eventData.putString("victim_vehicle_id", victim_vehicle_id);
		
		eventData.putString("weapon_id", weapon_id);
		
		eventData.putString("timestamp", timestamp);
		eventData.putString("zone_id", zone.getID());
		eventData.putString("facility_id", facility_id);
		eventData.putString("world_id", world.getID());
		
		//Filters
		JsonObject filterData = new JsonObject();
		
		filterData.putArray("characters", new JsonArray().addString(attacker_character.getCharacterID()).addString(victim_character.getCharacterID()));
		filterData.putArray("outfits", new JsonArray().addString(attacker_outfit_id).addString(victim_outfit_id));
		filterData.putArray("factions", new JsonArray().addString(attacker_faction.getId()).addString(victim_faction.getId()));
		filterData.putArray("loadouts", new JsonArray().addString(attacker_loadout_id));
		filterData.putArray("vehicles", new JsonArray().addString(attacker_vehicle_id).addString(victim_vehicle_id));
		filterData.putArray("weapons", new JsonArray().addString(weapon_id));
		filterData.putArray("facilities", new JsonArray().addString(facility_id));
		filterData.putArray("zones", new JsonArray().addString(zone.getID()));
		filterData.putArray("worlds", new JsonArray().addString(world.getID()));

		//Broadcast Event
		JsonObject message = new JsonObject();
		
		message.putObject("event_data", eventData);
		message.putObject("filter_data", filterData);
		message.putString("event_type", "VehicleDestroy");
		
		EventTracker.getInstance().getEventServer().BroadcastEvent(message);
	}
}
