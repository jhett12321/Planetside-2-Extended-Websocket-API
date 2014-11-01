package com.blackfeatherproductions.event_tracker;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonObject;
import com.blackfeatherproductions.event_tracker.events.Event;

public class QueryManager
{
	private Map<String, JsonObject> characterData = new HashMap<String,JsonObject>();
	private Map<String[], Event> callbacks;
	
	public void getCharacterData(String[] characterID, Event callbackEvent)
	{
		if(characterData.containsKey(characterID))
		{
			callbackEvent.queryCallback(characterData.get(characterID));
			return;
		}
		else
		{
			
		}
	}
}
