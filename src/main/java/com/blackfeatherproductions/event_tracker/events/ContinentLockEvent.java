package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.MetagameEventType;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;

@EventInfo(eventNames = "ContinentLock")
public class ContinentLockEvent implements Event
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
		//Event Specific Data
		String vs_population = payload.getString("vs_population");
		String nc_population = payload.getString("nc_population");
		String tr_population = payload.getString("tr_population");
		
		Faction triggering_faction = EventTracker.getInstance().getDataManager().getFactionByID(payload.getString("triggering_faction"));
		Faction previous_faction = EventTracker.getInstance().getDataManager().getFactionByID(payload.getString("previous_faction"));
		
		MetagameEventType metagame_event = EventTracker.getInstance().getDataManager().getMetagameEventTypeByID(payload.getString("metagame_event_id"));
		
		//Timestamp
		String timestamp = payload.getString("timestamp");
		
		//Location Data
		Zone zone = EventTracker.getInstance().getDataManager().getZoneByID(payload.getString("zone_id"));
		World world = EventTracker.getInstance().getDataManager().getWorldByID(payload.getString("world_id"));
	}
}
