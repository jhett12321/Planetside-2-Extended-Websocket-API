package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;
import com.blackfeatherproductions.event_tracker.data.dynamic.Character;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;


@EventInfo(eventNames = "Death")
public class CombatEvent implements Event
{
	private DataManager dataManager = EventTracker.getInstance().getDataManager();
	
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
			
			if(dataManager.getCharacterData().containsKey(attackerCharacterID) && dataManager.getCharacterData().containsKey(victimCharacterID))
			{
				processEvent();
			}
			
			else
			{
				String[] characterIDs = {attackerCharacterID, victimCharacterID};
				new CharacterQuery(characterIDs, this);
			}
		}
	}

	@Override
	public void processEvent()
	{
		//Event Specific Data
		String fire_mode_id = payload.getString("attacker_fire_mode_id");
		String vehicle_id = payload.getString("attacker_vehicle_id");
		String weapon_id = payload.getString("attacker_weapon_id");
		String is_headshot = payload.getString("is_headshot");
		
		String attacker_loadout_id = payload.getString("attacker_loadout_id");
		String victim_loadout_id = payload.getString("character_loadout_id");
		
		//Timestamp
		String timestamp = payload.getString("timestamp");
		
		//Location Data
		Zone zone = EventTracker.getInstance().getDataManager().getZoneByID(payload.getString("zone_id"));
		World world = EventTracker.getInstance().getDataManager().getWorldByID(payload.getString("world_id"));
		
		//Attacker Character Data
		Character attacker_character = dataManager.getCharacterData().get(attackerCharacterID);
		Faction attacker_faction = attacker_character.getFaction();
		String attacker_outfit_id = attacker_character.getOutfitID();
		
		//Victim Character Data
		Character victim_character = dataManager.getCharacterData().get(victimCharacterID);
		Faction victim_faction = victim_character.getFaction();
		String victim_outfit_id = victim_character.getOutfitID();
	}
}
