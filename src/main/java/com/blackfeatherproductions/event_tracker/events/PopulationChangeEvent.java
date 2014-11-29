package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

@EventInfo(eventNames = "PopulationChange")
public class PopulationChangeEvent implements Event
{
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		// TODO Merge Patch's Population Tracker into API
	}

	@Override
	public void processEvent()
	{
		
	}

}
