package com.blackfeatherproductions.event_tracker.queries;

import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.QueryManager;
import com.blackfeatherproductions.event_tracker.events.Event;

public class CharacterQuery implements Query
{
	private QueryManager queryManager = EventTracker.getInstance().getQueryManager();
	private Event callbackEvent;
	
	private List<String> characterIDs = new ArrayList<String>();
	
	public CharacterQuery(List<String> characterIDs, Event callbackEvent)
	{
		this.callbackEvent = callbackEvent;
		this.characterIDs.addAll(characterIDs);
		
		queryManager.addCharacterQuery(this);
	}
	
	public CharacterQuery(String characterID, Event callbackEvent)
	{
		this.callbackEvent = callbackEvent;
		this.characterIDs.add(characterID);
		
		queryManager.addCharacterQuery(this);
	}

	@Override
	public void ReceiveData(JsonObject data)
	{
		//We have already processed the character list, so just trigger the event.
		
		callbackEvent.processEvent();
	}

	public List<String> getCharacterIDs()
	{
		return characterIDs;
	}
}
