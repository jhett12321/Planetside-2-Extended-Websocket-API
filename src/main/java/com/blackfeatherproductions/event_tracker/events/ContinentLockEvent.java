package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;

@EventInfo(eventNames = "ContinentLock", priority = EventPriority.LOWEST)
public class ContinentLockEvent implements Event
{
	private DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
	
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
		
		Faction locked_by = Faction.getFactionByID(payload.getString("triggering_faction"));
		String metagame_event_id = payload.getString("metagame_event_id");
		
		String timestamp = payload.getString("timestamp");
		Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
		World world = World.getWorldByID(payload.getString("world_id"));
		
		//Payload
		JsonObject eventData = new JsonObject();
		
		eventData.putString("vs_population", vs_population);
		eventData.putString("nc_population", nc_population);
		eventData.putString("tr_population", tr_population);
		
		eventData.putString("locked_by", locked_by.getId());
		eventData.putString("metagame_event_id", metagame_event_id);
		
		eventData.putString("timestamp", timestamp);
		eventData.putString("zone_id", zone.getID());
		eventData.putString("world_id", world.getID());
		
		//Filters
		JsonObject filterData = new JsonObject();
		
		filterData.putArray("factions", new JsonArray().addString(locked_by.getId()));
		filterData.putArray("zones", new JsonArray().addString(zone.getID()));
		filterData.putArray("worlds", new JsonArray().addString(world.getID()));
		
		//Broadcast Event
		JsonObject message = new JsonObject();
		
		message.putObject("event_data", eventData);
		message.putObject("filter_data", filterData);
		message.putString("event_type", "ContinentLock");
		
		EventTracker.getInstance().getEventServer().BroadcastEvent(message);

		//Update Internal Data
		dynamicDataManager.getWorldData(world).getZoneInfo(zone).setLocked(true);
		dynamicDataManager.getWorldData(world).getZoneInfo(zone).setLockingFaction(locked_by);
	}
}
