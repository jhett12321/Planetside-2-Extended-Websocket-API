package com.blackfeatherproductions.event_tracker.events.extended;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;

@EventInfo(eventNames = "PopulationChange", priority = EventPriority.NORMAL)
public class PopulationChangeEvent implements Event
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
		//Payload
		JsonObject eventData = new JsonObject();
		
		eventData.putString("population_total", payload.getString("population_total"));
		eventData.putString("population_vs", payload.getString("population_vs"));
		eventData.putString("population_nc", payload.getString("population_nc"));
		eventData.putString("population_tr", payload.getString("population_tr"));
		eventData.putString("outfit_id", payload.getString("outfit_id"));
		eventData.putString("zone_id", payload.getString("zone_id"));
		eventData.putString("world_id", payload.getString("zone_id"));
			
		//Filters
		JsonObject filterData = new JsonObject();
		
		filterData.putArray("outfits", new JsonArray().addString(payload.getString("outfit_id")));
		filterData.putArray("zones", new JsonArray().addString(payload.getString("zone_id")));
		filterData.putArray("worlds", new JsonArray().addString(payload.getString("world_id")));
		
		//Broadcast Event
		JsonObject message = new JsonObject();
		
		message.putObject("event_data", eventData);
		message.putObject("filter_data", filterData);
		message.putString("event_type", "PopulationChangeEvent");
		
		EventTracker.getInstance().getEventServer().BroadcastEvent(message);
	}

}
