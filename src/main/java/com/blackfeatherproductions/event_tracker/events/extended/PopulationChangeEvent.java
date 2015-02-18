package com.blackfeatherproductions.event_tracker.events.extended;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;

@EventInfo(eventNames = "PopulationChange", priority = EventPriority.NORMAL)
public class PopulationChangeEvent implements Event
{
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		// TODO 1.1 Merge Patch's Population Tracker into API
		// See PopulationTracker.java
	}

	@Override
	public void processEvent()
	{
		
	}

}
