package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;

@EventInfo(eventNames = "ContinentLock")
public class ContinentLockEvent implements Event
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
		//Data
		String vs_population = payload.getString("vs_population");
		String nc_population = payload.getString("nc_population");
		String tr_population = payload.getString("tr_population");
		
		Faction locked_by = dataManager.getFactionByID(payload.getString("triggering_faction"));
		//MetagameEventType metagame_event = dataManager.getMetagameEventTypeByID(payload.getString("metagame_event_id"));
		String metagame_event_id = payload.getString("metagame_event_id");
		
		String timestamp = payload.getString("timestamp");
		Zone zone = dataManager.getZoneByID(payload.getString("zone_id"));
		World world = dataManager.getWorldByID(payload.getString("world_id"));
		
		//Message
		JsonObject eventData = new JsonObject();
		
		eventData.putString("vs_population", vs_population);
		eventData.putString("nc_population", nc_population);
		eventData.putString("tr_population", tr_population);
		
		eventData.putString("locked_by", locked_by.getId());
		eventData.putString("metagame_event_id", metagame_event_id);
		
		eventData.putString("timestamp", timestamp);
		eventData.putString("zone_id", zone.getID());
		eventData.putString("world_id", world.getID());
		
		//Filter
		JsonObject filterData = new JsonObject();
		
		filterData.putArray("factions", new JsonArray().addString(locked_by.getId()));
		filterData.putArray("zones", new JsonArray().addString(zone.getID()));
		filterData.putArray("worlds", new JsonArray().addString(world.getID()));
	}
}
