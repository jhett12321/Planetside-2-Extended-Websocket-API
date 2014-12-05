package com.blackfeatherproductions.event_tracker.data.dynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blackfeatherproductions.event_tracker.data.Zone;

public class WorldInfo
{
	private Map<Zone, ZoneInfo> zones = new HashMap<Zone, ZoneInfo>();
	private List<MetagameEventInfo> metagameEvents = new ArrayList<MetagameEventInfo>();

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
	
	public List<MetagameEventInfo> getActiveMetagameEvents()
	{
		return metagameEvents;
	}
}
