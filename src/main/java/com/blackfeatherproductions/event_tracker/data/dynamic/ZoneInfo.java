package com.blackfeatherproductions.event_tracker.data.dynamic;

import java.util.HashMap;
import java.util.Map;

import com.blackfeatherproductions.event_tracker.data.Faction;

public class ZoneInfo
{
	private Boolean locked;
	private Faction locking_faction;
	
	//TODO 1.1/1.2 Implement Facility Class to replace String Facility ID lookup
	private Map<String, FacilityInfo> facilities = new HashMap<String, FacilityInfo>();
	
	public FacilityInfo getFacility(String facility_id)
	{
		return facilities.get(facility_id);
	}
	
	public Map<String, FacilityInfo> getFacilities()
	{
		return facilities;
	}

	public void setLocked(Boolean locked)
	{
		this.locked = locked;
	}
	
	public void setLockingFaction(Faction locking_faction)
	{
		this.locking_faction = locking_faction;
	}
}
