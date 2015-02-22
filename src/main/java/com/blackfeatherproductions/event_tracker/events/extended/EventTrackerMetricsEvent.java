package com.blackfeatherproductions.event_tracker.events.extended;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;

@EventInfo(eventName="EventTrackerMetrics",
listenedEvents = "EventTrackerMetrics",
priority = EventPriority.NORMAL,
filters = { "no_filtering" })
public class EventTrackerMetricsEvent implements Event
{
	private JsonObject payload;
	
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		this.payload = payload;
		
		processEvent();
	}

	@Override
	public void processEvent()
	{
		//Broadcast Event
		JsonObject message = new JsonObject();
		
		message.putObject("event_data", payload);
		message.putObject("filter_data", null);
		
		EventTracker.getInstance().getEventServer().BroadcastEvent(this.getClass(), message);
	}

}
