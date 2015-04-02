package com.blackfeatherproductions.event_tracker.events.extended;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;

@EventInfo(eventName="ServerStatusChange",
listenedEvents = "ServerStatusChange",
priority = EventPriority.NORMAL,
filters = { "worlds" })
public class ServerStatusChangeEvent implements Event
{
	private final EventTracker eventTracker = EventTracker.getInstance();
	
	private JsonObject payload;
	
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		this.payload = payload;
		if(payload != null)
		{
			processEvent();
		}
	}

	@Override
	public void processEvent()
	{
		JsonObject eventData = new JsonObject();
		
		//Event Specific Data
		eventData.putString("old_time", payload.getString("old_time"));
		eventData.putString("new_time", payload.getString("new_time"));
		eventData.putString("diff", payload.getString("diff"));
		
		JsonObject message = new JsonObject();
		message.putObject("event_data", eventData);
		
		eventTracker.getEventServer().BroadcastEvent(this.getClass(), message);
	}

}
