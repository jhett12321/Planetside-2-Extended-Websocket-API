package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventNames = "PlayerLogin|PlayerLogout", priority = EventPriority.NORMAL)
public class LoginEvent implements Event
{
	private DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
	
	private JsonObject payload;
	
	private String characterID;
	
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		this.payload = payload;
		if(payload != null)
		{
			characterID = payload.getString("character_id");
			
			if(dynamicDataManager.characterDataExists(characterID))
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
		String event_name = payload.getString("event_name");
		
		//Data
		CharacterInfo character = dynamicDataManager.getCharacterData(characterID);
		String outfit_id = character.getOutfitID();
		Faction faction = character.getFaction();
		
		String is_login = "0";
		if(event_name == "PlayerLogin")
		{
			is_login = "1";
		}
		
		String timestamp = payload.getString("timestamp");
		World world = World.getWorldByID(payload.getString("world_id"));
		
		//Payload
		JsonObject eventData = new JsonObject();
		
		eventData.putString("character_id", character.getCharacterID());
		eventData.putString("outfit_id", outfit_id);
		eventData.putString("faction_id", faction.getId());
		eventData.putString("is_login", is_login);
		eventData.putString("timestamp", timestamp);
		eventData.putString("world_id", world.getID());
		
		//Filters
		JsonObject filterData = new JsonObject();
		
		filterData.putArray("characters", new JsonArray().addString(character.getCharacterID()));
		filterData.putArray("outfits", new JsonArray().addString(outfit_id));
		filterData.putArray("factions", new JsonArray().addString(faction.getId()));
		filterData.putArray("login_types", new JsonArray().addString(is_login));
		filterData.putArray("worlds", new JsonArray().addString(world.getID()));
		
		//Broadcast Event
		JsonObject message = new JsonObject();
		
		message.putObject("event_data", eventData);
		message.putObject("filter_data", filterData);
		message.putString("event_type", "DirectiveCompleted");
		
		EventTracker.getInstance().getEventServer().BroadcastEvent(message);
	}
}
