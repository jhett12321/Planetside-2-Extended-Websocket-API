package com.blackfeatherproductions.event_tracker.queries;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.StaticDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;

public class CharacterQuery implements Query
{
	private StaticDataManager staticDataManager = EventTracker.getInstance().getStaticDataManager();
	private DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
	private Event callbackEvent;
	
	public CharacterQuery(String[] characterIDs, Event callbackEvent)
	{
		this.callbackEvent = callbackEvent;
		
		for(String character : characterIDs)
		{
			//TODO Census Query
		}
	}
	
	public CharacterQuery(String characterID, Event callbackEvent)
	{
		this.callbackEvent = callbackEvent;
		//TODO Census Query
	}

	@Override
	public void ReceiveData(JsonObject data)
	{
		callbackEvent.processEvent();
	}
}
