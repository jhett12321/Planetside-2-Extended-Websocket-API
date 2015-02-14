package com.blackfeatherproductions.event_tracker.data.dynamic;

import java.util.HashMap;
import java.util.Map;

import com.blackfeatherproductions.event_tracker.data.Zone;

public class WorldInfo
{
	private Map<Zone, ZoneInfo> zones = new HashMap<Zone, ZoneInfo>();
	private Map<String, MetagameEventInfo> metagameEvents = new HashMap<String, MetagameEventInfo>();

	//This is the main class for world-centric data.
	//The DynamicDataManager allows the system to get the world instance.
	
	public ZoneInfo getZoneInfo(Zone zone)
	{
		return zones.get(zone);
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
}
