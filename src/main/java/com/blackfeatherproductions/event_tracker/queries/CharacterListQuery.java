package com.blackfeatherproductions.event_tracker.queries;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data.dynamic.CharacterInfo;

public class CharacterListQuery implements Query
{
	private DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
	
	@Override
	public void ReceiveData(JsonObject data)
	{
		JsonArray characterList = data.getArray("character_list");
		
		for(int i=0; i<characterList.size(); i++)
		{
			JsonObject characterData = characterList.get(i);
			
			String characterID = characterData.getString("character_id");
			String factionID = characterData.getString("faction_id");
			String outfitID = characterData.getObject("outfit").getString("outfit_id");
			
			CharacterInfo character = new CharacterInfo(characterID, factionID, outfitID);
			
			dynamicDataManager.addCharacterData(characterID, character);
		}
	}

}
