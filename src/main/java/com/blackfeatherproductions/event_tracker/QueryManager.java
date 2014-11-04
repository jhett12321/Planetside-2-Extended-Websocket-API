package com.blackfeatherproductions.event_tracker;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.feeds.Census;

public class QueryManager
{
	private Map<String, JsonObject> characterData = new HashMap<String,JsonObject>();
	private Map<String[], Event> callbacks;
	
	public void getCharacterData(String[] characterID, Event callbackEvent)
	{
		//TODO
		
		for(String character : characterID)
		{
			if(characterData.containsKey(character))
			{
				callbackEvent.queryCallback(characterData.get(character));
				return;
			}
		}
	}
	
	public void getWorldData(String[] worldIDs, Census callbackWebsocket)
	{
		
	}
}
