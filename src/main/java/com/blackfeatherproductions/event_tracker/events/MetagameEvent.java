package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;

@EventInfo(eventNames = "MetagameEvent|FacilityControl")
public class MetagameEvent implements Event
{
	private DataManager dataManager = EventTracker.getInstance().getDataManager();
	
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
		//TODO
		/*
		String event_name = payload.getString("event_name");
		
		if(event_name == "FacilityControl")
		{
			//TODO check if facility is a part of an alert.
		}
		
		else
		{
		
			//Data
			
			//Message
			JsonObject eventData = new JsonObject();
			eventData.putString("instance_id", instance_id);
			eventData.putString("type_id", instance_id);
			eventData.putString("start_time", instance_id);
			eventData.putString("end_time", "0");
			eventData.putString("timestamp", timestamp);
			eventData.putString("status", status);
			eventData.putString("control_vs", control_vs);
			eventData.putString("control_nc", control_nc);
			eventData.putString("control_tr", control_tr);
			eventData.putObject("facility_captured", facility_captured);
			eventData.putString("domination", domination);
			eventData.putString("zone_id", zone.getID());
			eventData.putString("world_id", world.getID());
		}
		*/
	}
}
