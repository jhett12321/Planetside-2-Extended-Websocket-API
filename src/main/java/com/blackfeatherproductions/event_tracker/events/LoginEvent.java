package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;
import com.blackfeatherproductions.event_tracker.data.dynamic.Character;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventNames = "PlayerLogin|PlayerLogout")
public class LoginEvent implements Event
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
		//Event Specific Data
		String event_name = payload.getString("event_name");
		String is_login;
		
		if(event_name == "PlayerLogin")
		{
			is_login = "1";
		}
		else if(event_name == "PlayerLogout")
		{
			is_login = "0";
		}
		
		//Timestamp
		String timestamp = payload.getString("timestamp");
		
		//Location Data
		Zone zone = EventTracker.getInstance().getDataManager().getZoneByID(payload.getString("zone_id"));
		World world = EventTracker.getInstance().getDataManager().getWorldByID(payload.getString("world_id"));
		
		//Character Data
		Character character = dataManager.getCharacterData().get(characterID);
		Faction faction = character.getFaction();
		String outfit_id = character.getOutfitID();
	}
}
