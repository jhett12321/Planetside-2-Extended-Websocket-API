package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data.Character;
import com.blackfeatherproductions.event_tracker.game_data.Faction;
import com.blackfeatherproductions.event_tracker.game_data.World;
import com.blackfeatherproductions.event_tracker.game_data.Zone;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventNames = "DirectiveCompleted")
public class DirectiveCompletedEvent implements Event
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
		String directive_id = payload.getString("directive_id");
		
		//Timestamp
		String timestamp = payload.getString("timestamp");
		
		//Location Data
		Zone zone = EventTracker.getInstance().getGameData().getZoneByID(payload.getString("zone_id"));
		World world = EventTracker.getInstance().getGameData().getWorldByID(payload.getString("world_id"));
		
		//Character Data
		Character character = dataManager.getCharacterData().get(characterID);
		Faction faction = character.getFaction();
		String outfit_id = character.getOutfitID();
	}
}
