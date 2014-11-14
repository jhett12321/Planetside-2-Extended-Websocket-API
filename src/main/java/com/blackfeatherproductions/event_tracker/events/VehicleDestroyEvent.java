package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

@EventInfo(eventNames = "VehicleDestroy")
public class VehicleDestroyEvent implements Event
{
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void processEvent()
	{
		// TODO Auto-generated method stub
	}
}
