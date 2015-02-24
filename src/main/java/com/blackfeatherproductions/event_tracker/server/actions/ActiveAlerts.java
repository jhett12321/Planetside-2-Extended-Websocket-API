package com.blackfeatherproductions.event_tracker.server.actions;

import java.util.Map.Entry;

import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.World;

@ActionInfo(actionNames = "activeAlerts")
public class ActiveAlerts implements Action
{
	private DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
	
	@Override
	public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
	{
		JsonObject response = new JsonObject();
		response.putString("action", "activeAlerts");
		
		JsonObject worlds = new JsonObject();
		
		if(actionData.containsField("worlds"))
		{
			for(int i=0; i<actionData.getArray("worlds").size(); i++)
			{
				if(Utils.isValidWorld((String) actionData.getArray("worlds").get(i)))
				{
					World world = World.getWorldByID((String) actionData.getArray("worlds").get(i));
					
					JsonObject metagameEvents = new JsonObject();

					for(MetagameEventInfo metagameEventInfo : dynamicDataManager.getWorldInfo(world).getActiveMetagameEvents().values())
					{
						JsonObject metagameEvent = new JsonObject();
						
						metagameEvent.putString("instance_id", metagameEventInfo.getInstanceID());
						metagameEvent.putString("start_time", metagameEventInfo.getStartTime());
						metagameEvent.putString("end_time", metagameEventInfo.getEndTime());
						metagameEvent.putString("type_id", metagameEventInfo.getType().getTypeID());
						
						JsonObject facilities = new JsonObject();
						
						for(Entry<Facility, FacilityInfo> facilityInfo : dynamicDataManager.getWorldInfo(world).getZoneInfo(metagameEventInfo.getType().getZone()).getFacilities().entrySet())
						{
							JsonObject facility = new JsonObject();
							
							facility.putString("facility_id", facilityInfo.getKey().getID());
							facility.putString("facility_type_id", facilityInfo.getKey().getTypeID());
							facility.putString("owner", facilityInfo.getValue().getOwner().getID());
							facility.putString("zone_id", metagameEventInfo.getType().getZone().getID());
							
							facilities.putObject(facilityInfo.getKey().getID(), facility);
						}
						
						metagameEvent.putObject("facilities", facilities);
						
						metagameEvents.putObject(metagameEventInfo.getInstanceID(), metagameEvent);
					}
					
					worlds.putObject(world.getID(), metagameEvents);
				}
			}
		}
		else
		{
			for(World world : World.getValidWorlds())
			{
				JsonObject metagameEvents = new JsonObject();

				for(MetagameEventInfo metagameEventInfo : dynamicDataManager.getWorldInfo(world).getActiveMetagameEvents().values())
				{
					JsonObject metagameEvent = new JsonObject();
					
					metagameEvent.putString("instance_id", metagameEventInfo.getInstanceID());
					metagameEvent.putString("start_time", metagameEventInfo.getStartTime());
					metagameEvent.putString("end_time", metagameEventInfo.getEndTime());
					metagameEvent.putString("type_id", metagameEventInfo.getType().getTypeID());
					
					JsonObject facilities = new JsonObject();
					
					for(Entry<Facility, FacilityInfo> facilityInfo : dynamicDataManager.getWorldInfo(world).getZoneInfo(metagameEventInfo.getType().getZone()).getFacilities().entrySet())
					{
						JsonObject facility = new JsonObject();
						
						facility.putString("facility_id", facilityInfo.getKey().getID());
						facility.putString("facility_type_id", facilityInfo.getKey().getTypeID());
						facility.putString("owner", facilityInfo.getValue().getOwner().getID());
						facility.putString("zone_id", metagameEventInfo.getType().getZone().getID());
						
						facilities.putObject(facilityInfo.getKey().getID(), facility);
					}
					
					metagameEvent.putObject("facilities", facilities);
					
					metagameEvents.putObject(metagameEventInfo.getInstanceID(), metagameEvent);
				}
				
				worlds.putObject(world.getID(), metagameEvents);
			}
		}
		
		//Send Client Response
		response.putObject("worlds", worlds);
		
		clientConnection.writeTextFrame(response.encode());
	}

}
