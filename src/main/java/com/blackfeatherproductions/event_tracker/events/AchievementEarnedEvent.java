package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;
import com.blackfeatherproductions.event_tracker.data.dynamic.Character;
import com.blackfeatherproductions.event_tracker.DataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventNames = "AchievementEarned")
public class AchievementEarnedEvent implements Event
{
	private DataManager dataManager = EventTracker.getInstance().getDataManager();
	
	private JsonObject payload;
	
	private String characterID;
	
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		this.payload = payload;
		if(payload != null)
		{
			characterID = payload.getString("character_id");
			
			if(dataManager.getCharacterData().containsKey(characterID))
			{	
				processEvent();
			}
			
			else
			{
				new CharacterQuery(characterID, this);
			}
		}
	}

	@Override
	public void processEvent()
	{
		JsonObject eventData = new JsonObject();
		
		//Event Specific Data
		String achievement_id = payload.getString("achievement_id");
		
		//Timestamp
		String timestamp = payload.getString("timestamp");
		
		//Location Data
		Zone zone = dataManager.getZoneByID(payload.getString("zone_id"));
		World world = dataManager.getWorldByID(payload.getString("world_id"));
		
		//Character Data
		Character character = dataManager.getCharacterData().get(characterID);
		Faction faction = character.getFaction();
		String outfit_id = character.getOutfitID();
		
		//Raw ID Data
			//Event Specific Data
		eventData.putString("achievement_id", achievement_id);
		
			//Character Data
		eventData.putString("character_id", character.getCharacterID());
		eventData.putString("faction_id", faction.getId());
		eventData.putString("outfit_id", outfit_id);
		
			//Timestamp
		eventData.putString("timestamp", timestamp);
		
			//Location Data
		eventData.putString("zone_id", zone.getID());
		eventData.putString("world_id", world.getID());
		
		//TODO Translated Message Object - See GameData.java
		
		//Broadcast Event Data
		JsonObject message = new JsonObject();
		
		message.putObject("event_data", eventData);
		message.putString("event_type", "AchievementEarned");
		
		EventTracker.getInstance().getEventServer().BroadcastEvent(message);
	}
}
