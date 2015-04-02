package com.blackfeatherproductions.event_tracker.events.extended;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;

@EventInfo(eventName="PopulationChange",
listenedEvents = "PopulationChange",
priority = EventPriority.NORMAL,
filters = { "population_types", "outfits", "zones", "worlds" })
public class PopulationChangeEvent implements Event
{
	private final EventTracker eventTracker = EventTracker.getInstance();
	
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
		//Payload & Filters
		JsonObject eventData = new JsonObject();
		JsonObject filterData = new JsonObject();
		
		String populationType = payload.getString("population_type");
		
		eventData.putString("population_type", populationType);
		filterData.putArray("population_types", new JsonArray().addString(populationType));
		
		eventData.putString("population_total", payload.getString("population_total"));
		
		if(populationType.equals("total") || populationType.equals("world") || populationType.equals("zone"))
		{
			eventData.putString("population_vs", payload.getString("population_vs"));
			eventData.putString("population_nc", payload.getString("population_nc"));
			eventData.putString("population_tr", payload.getString("population_tr"));
		}
		
		if(populationType.equals("outfit") || populationType.equals("zone_outfit"))
		{
			eventData.putString("outfit_id", payload.getString("outfit_id"));
			filterData.putArray("outfits", new JsonArray().addString(payload.getString("outfit_id")));
		}
		
		if(populationType.equals("zone") || populationType.equals("zone_outfit"))
		{
			eventData.putString("zone_id", payload.getString("zone_id"));
			filterData.putArray("zones", new JsonArray().addString(payload.getString("zone_id")));
		}

		if(populationType.equals("world") || populationType.equals("zone") || populationType.equals("outfit") || populationType.equals("zone_outfit"))
		{
			eventData.putString("world_id", payload.getString("world_id"));
			filterData.putArray("worlds", new JsonArray().addString(payload.getString("world_id")));
		}
		
		//Broadcast Event
		JsonObject message = new JsonObject();
		
		message.putObject("event_data", eventData);
		message.putObject("filter_data", filterData);
		
		eventTracker.getEventServer().BroadcastEvent(this.getClass(), message);
	}

}
