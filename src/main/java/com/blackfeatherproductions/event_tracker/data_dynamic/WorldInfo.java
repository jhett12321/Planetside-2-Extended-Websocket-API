package com.blackfeatherproductions.event_tracker.data_dynamic;

import java.util.HashMap;
import java.util.Map;

import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class WorldInfo
{
	private Map<Zone, ZoneInfo> zones = new HashMap<Zone, ZoneInfo>();
	private Map<String, MetagameEventInfo> metagameEvents = new HashMap<String, MetagameEventInfo>();
	
	private boolean online = false;

	//This is the main class for world-centric data.
	//The DynamicDataManager allows the system to get the world instance.
	public ZoneInfo getZoneInfo(Zone zone)
	{
		if(zone != null)
		{
			if(zones.get(zone) == null)
			{
				zones.put(zone, new ZoneInfo());
			}
			
			return zones.get(zone);
		}
		
		return null;
	}
	
	public Map<Zone, ZoneInfo> getZones()
	{
		return zones;
	}
	
	public Map<String, MetagameEventInfo> getActiveMetagameEvents()
	{
		return metagameEvents;
	}
	
	public MetagameEventInfo getActiveMetagameEvent(String instance_id)
	{
		return metagameEvents.get(instance_id);
	}

	public boolean isOnline()
	{
		return online;
	}

	public void setOnline(boolean online)
	{
		this.online = online;
	}
}
