package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

@EventInfo(eventNames = "DirectiveCompleted")
public class DirectiveCompletedEvent implements Event
{
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void processEvent(JsonObject payload)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void queryCallback(JsonObject queriedData)
	{
		// TODO Auto-generated method stub
		
	}
}
