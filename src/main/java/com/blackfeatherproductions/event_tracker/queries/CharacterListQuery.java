package com.blackfeatherproductions.event_tracker.queries;

import java.util.List;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;

public class CharacterListQuery implements Query
{
	private DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
	private List<CharacterQuery> callbacks;
	
	public CharacterListQuery(List<CharacterQuery> callbacks)
	{
		this.callbacks = callbacks;
	}

	@Override
	public void ReceiveData(JsonObject data)
	{
		JsonArray characterList = data.getArray("character_list");
		
		for(int i=0; i<characterList.size(); i++)
		{
			JsonObject characterData = characterList.get(i);
			
			String characterID = characterData.getString("character_id");
			String characterName = characterData.getObject("name").getString("first");
			String factionID = characterData.getString("faction_id");
			
			String outfitID;
			String zoneID;
			
			if(characterData.containsField("outfit"))
			{
				outfitID = characterData.getObject("outfit").getString("outfit_id");
			}
			else
			{
				outfitID = "0";
			}
			
			if(characterData.containsField("last_event"))
			{
				zoneID = characterData.getObject("last_event").getString("zone_id");
			}
			else
			{
				zoneID = "0";
			}
			
			CharacterInfo character = new CharacterInfo(characterID, characterName, factionID, outfitID, zoneID);
			
			dynamicDataManager.addCharacterData(characterID, character);
		}
		
		for(CharacterQuery event : callbacks)
		{
			event.ReceiveData(null); //Triggers the waiting events for processing.
		}
	}

}
