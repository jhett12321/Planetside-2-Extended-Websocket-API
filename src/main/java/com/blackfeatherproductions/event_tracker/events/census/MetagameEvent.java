package com.blackfeatherproductions.event_tracker.events.census;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.MetagameEventType;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;

@EventInfo(eventNames = "MetagameEvent|FacilityControl", priority = EventPriority.HIGHEST)
public class MetagameEvent implements Event
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
			//Shared
		String event_name = payload.getString("event_name");
		String timestamp = payload.getString("timestamp");
		
		World world = World.getWorldByID(payload.getString("world_id"));
		WorldInfo worldData = dynamicDataManager.getWorldInfo(world);
		
			//Data to be resolved
		String instance_id = null;
		String type_id = null;
		String start_time = "0";
		String end_time = "0";
		String status = null;
		String domination = "0";
		
		Zone zone = null;
		
			//Facility Control Data
		if(event_name.equals("FacilityControl"))
		{
			zone = Zone.getZoneByID(payload.getString("zone_id"));
			
			MetagameEventInfo metagameEventInfo = null;
			for(MetagameEventInfo info : worldData.getActiveMetagameEvents().values())
			{
				if(info.getType().getZone() == zone)
				{
					metagameEventInfo = info;
				}
			}
			
			if(metagameEventInfo == null)
			{
				return; //No alert is running in this zone.
			}
			
			else
			{
				instance_id = metagameEventInfo.getInstanceID();
				type_id = metagameEventInfo.getType().getTypeID();
				start_time = metagameEventInfo.getStartTime();
				end_time = metagameEventInfo.getEndTime();
				status = "2";
				domination = "0";
			}
		}
		
			//MetagameEventData
		else
		{
			MetagameEventType metagameEventType = MetagameEventType.getMetagameEventTypeByID(payload.getString("metagame_event_id"));
			
			zone = metagameEventType.getZone();
			
			instance_id = payload.getString("instance_id");
			type_id = metagameEventType.getTypeID();
			
			//Alert End (138->ended, 137->canceled, 136->restarted)
			if(payload.getString("metagame_event_state").equals("138") || payload.getString("metagame_event_state").equals("137") || payload.getString("metagame_event_state").equals("136"))
			{
				MetagameEventInfo metagameEventInfo = worldData.getActiveMetagameEvent(instance_id);
				
				start_time = metagameEventInfo.getStartTime();
				end_time = timestamp;
				status = "0";
				
				//Remove event from tracking list.
				worldData.removeMetagameEvent(instance_id);
			}
			
			//Alert Start (135->started, 136->restarted)
			if(payload.getString("metagame_event_state").equals("135") || payload.getString("metagame_event_state").equals("136"))
			{
				start_time = timestamp;
				end_time = String.valueOf((Integer.valueOf(timestamp) + 7200));
				status = "1";
				
				//Create a new Metagame Event
				worldData.addMetagameEvent(instance_id, new MetagameEventInfo(instance_id, metagameEventType, start_time, end_time));
			}
		}
		
			//Territory Control
		JsonObject controlInfo = Utils.calculateTerritoryControl(world, zone);
		String control_vs = controlInfo.getString("control_vs");
		String control_nc = controlInfo.getString("control_nc");
		String control_tr = controlInfo.getString("control_tr");
		
		if(Integer.valueOf(control_vs) >= 100 || Integer.valueOf(control_nc) >= 100 || Integer.valueOf(control_tr) >= 100)
		{
			domination = "1";
		}
			
		//Payload
		JsonObject eventData = new JsonObject();
		
		eventData.putString("instance_id", instance_id);
		eventData.putString("type_id", type_id);
		eventData.putString("start_time", start_time);
		eventData.putString("end_time", "0");
		eventData.putString("timestamp", timestamp);
		eventData.putString("status", status);
		eventData.putString("control_vs", control_vs);
		eventData.putString("control_nc", control_nc);
		eventData.putString("control_tr", control_tr);
		eventData.putString("domination", domination);
		eventData.putString("zone_id", zone.getID());
		eventData.putString("world_id", world.getID());
			
		//Filters
		JsonObject filterData = new JsonObject();
		
		filterData.putArray("metagames", new JsonArray().addString(instance_id));
		filterData.putArray("metagame_types", new JsonArray().addString(type_id));
		filterData.putArray("statuses", new JsonArray().addString(status));
		filterData.putArray("dominations", new JsonArray().addString(domination));
		filterData.putArray("zones", new JsonArray().addString(zone.getID()));
		filterData.putArray("worlds", new JsonArray().addString(world.getID()));
		
		//Broadcast Event
		JsonObject message = new JsonObject();
		
		message.putObject("event_data", eventData);
		message.putObject("filter_data", filterData);
		message.putString("event_type", "MetagameEvent");
		
		EventTracker.getInstance().getEventServer().BroadcastEvent(message);
	}
}
