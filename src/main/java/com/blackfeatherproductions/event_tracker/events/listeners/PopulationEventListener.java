package com.blackfeatherproductions.event_tracker.events.listeners;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.events.Event;

public class PopulationEventListener implements Event
{
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		processEvent();
	}

	@Override
	public void processEvent()
	{
		
	}
	

}
