package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;

@EventInfo(eventNames = "FacilityControl")
public class FacilityControlEvent implements Event
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
		String facility_id = payload.getString("facility_id");
		String duration_held = payload.getString("duration_held");
		
		String new_faction_id = payload.getString("new_faction_id");
		String old_faction_id = payload.getString("old_faction_id");
		
		String is_capture = "0";
		if(new_faction_id.equals(old_faction_id))
		{
			is_capture = "1";
		}
		
		//TODO % Territory Control
		
		String timestamp = payload.getString("timestamp");
		Zone zone = dataManager.getZoneByID(payload.getString("zone_id"));
		World world = dataManager.getWorldByID(payload.getString("world_id"));
		
		//Message
		JsonObject eventData = new JsonObject();
		
		eventData.putString("facility_id", facility_id);
		eventData.putString("duration_held", duration_held);
		
		eventData.putString("new_faction_id", new_faction_id);
		eventData.putString("old_faction_id", old_faction_id);
		
		eventData.putString("is_capture", is_capture);
		
		//TODO
		//eventData.putString("control_vs", control_vs);
		//eventData.putString("control_nc", control_nc);
		//eventData.putString("control_tr", control_tr);
		
		//eventData.putString("timestamp", timestamp);
		//eventData.putString("zone_id", zone.getID());
		//eventData.putString("world_id", world.getID());
		
		//Filter
		JsonObject filterData = new JsonObject();
		
		filterData.putArray("facilities", new JsonArray().addString(facility_id));
		//filterData.putArray("facility_types", new JsonArray().addString(facility_type_id));
		filterData.putArray("factions", new JsonArray().addString(new_faction_id).addString(old_faction_id));
		filterData.putArray("captures", new JsonArray().addString(is_capture));
		filterData.putArray("zones", new JsonArray().addString(zone.getID()));
		filterData.putArray("worlds", new JsonArray().addString(world.getID()));
	}
}
