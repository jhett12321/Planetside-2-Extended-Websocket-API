package com.blackfeatherproductions.event_tracker.queries;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;

public class MetagameEventQuery implements Query
{
	private EventTracker eventTracker = EventTracker.getInstance();
	public MetagameEventQuery(String worldID)
	{
		String timestamp = String.valueOf(Math.round(new Date().getTime() / 1000) - 7201);
		
		eventTracker.getQueryManager().getCensusData("/get/ps2:v2/world_event/?type=METAGAME&c:limit=100&c:lang=en&world_id=" + worldID + "&after=" + timestamp, false, this);
	}

	@Override
	public void ReceiveData(JsonObject data)
	{
		JsonArray eventArray = data.getArray("world_event_list");
		
		List<String> finishedEvents = new ArrayList<String>();
		
		for(int i=0; i<eventArray.size(); i++)
		{
			JsonObject event = eventArray.get(i);
			
			String eventState = event.getString("metagame_event_state");
			String instanceID = event.getString("instance_id");
			
			if(eventState.equals("137") || eventState.equals("138"))
			{
				finishedEvents.add(instanceID);
			}
		}
		
		for(int i=0; i<eventArray.size(); i++)
		{
			JsonObject event = eventArray.get(i);
			
			String eventState = event.getString("metagame_event_state");
			String instanceID = event.getString("instance_id");
			
			if((eventState.equals("135") || eventState.equals("136")) && !finishedEvents.contains(instanceID))
			{
				//Process Dummy Event Message
				JsonObject payload = new JsonObject();
				payload.putString("event_name", "MetagameEvent");
				payload.putString("instance_id", instanceID);
				payload.putString("metagame_event_id", event.getString("metagame_event_id"));
				payload.putString("metagame_event_state", eventState);
				payload.putString("timestamp", event.getString("timestamp"));
				payload.putString("world_id", event.getString("world_id"));
				
                String eventName = payload.getString("event_name");
                
                eventTracker.getEventHandler().handleEvent(eventName, payload);
			}
		}
	}
	

}
