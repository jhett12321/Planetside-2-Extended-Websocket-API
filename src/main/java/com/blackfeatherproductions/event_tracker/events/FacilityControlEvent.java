package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data.Facility;
import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;

@EventInfo(eventNames = "FacilityControl")
public class FacilityControlEvent implements Event
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
		String facility_id = payload.getString("facility_id");
		Facility facility = Facility.getFacilityByID(facility_id);
		String duration_held = payload.getString("duration_held");
		
		Faction new_faction = Faction.getFactionByID(payload.getString("new_faction_id"));
		Faction old_faction = Faction.getFactionByID(payload.getString("old_faction_id"));
		
		String is_capture = "0";
		if(new_faction == old_faction)
		{
			is_capture = "1";
		}
		
		String timestamp = payload.getString("timestamp");
		Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
		World world = World.getWorldByID(payload.getString("world_id"));
		
		//Territory Control
		JsonObject controlInfo = Utils.calculateTerritoryControl(world, zone);
		String control_vs = controlInfo.getString("control_vs");
		String control_nc = controlInfo.getString("control_nc");
		String control_tr = controlInfo.getString("control_tr");
		
		//Payload
		JsonObject eventData = new JsonObject();
		
		eventData.putString("facility_id", facility_id);
		eventData.putString("facility_type_id", facility.getTypeID());
		eventData.putString("duration_held", duration_held);
		
		eventData.putString("new_faction_id", new_faction.getId());
		eventData.putString("old_faction_id", old_faction.getId());
		
		eventData.putString("is_capture", is_capture);
		
		eventData.putString("control_vs", control_vs);
		eventData.putString("control_nc", control_nc);
		eventData.putString("control_tr", control_tr);
		
		eventData.putString("timestamp", timestamp);
		eventData.putString("zone_id", zone.getID());
		eventData.putString("world_id", world.getID());
		
		//Filters
		JsonObject filterData = new JsonObject();
		
		filterData.putArray("facilities", new JsonArray().addString(facility_id));
		filterData.putArray("facility_types", new JsonArray().addString(facility.getTypeID()));
		filterData.putArray("factions", new JsonArray().addString(new_faction.getId()).addString(old_faction.getId()));
		filterData.putArray("captures", new JsonArray().addString(is_capture));
		filterData.putArray("zones", new JsonArray().addString(zone.getID()));
		filterData.putArray("worlds", new JsonArray().addString(world.getID()));
		
		//Broadcast Event
		JsonObject message = new JsonObject();
		
		message.putObject("event_data", eventData);
		message.putObject("filter_data", filterData);
		message.putString("event_type", "ContinentLock");
		
		EventTracker.getInstance().getEventServer().BroadcastEvent(message);

		//Update Internal Data
		if(is_capture == "1")
		{
			dynamicDataManager.getWorldData(world).getZoneInfo(zone).getFacility(Facility.getFacilityByID(facility_id)).setOwner(new_faction);
		}
	}
}
