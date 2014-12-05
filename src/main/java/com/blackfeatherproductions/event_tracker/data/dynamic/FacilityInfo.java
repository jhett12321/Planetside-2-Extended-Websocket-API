package com.blackfeatherproductions.event_tracker.data.dynamic;

import com.blackfeatherproductions.event_tracker.data.Faction;

public class FacilityInfo
{
	private Faction owner;
	
	public FacilityInfo(Faction owner)
	{
		this.owner = owner;
	}
	
	public Faction getOwner()
	{
		return owner;
	}
	
	public void setOwner(Faction owner)
	{
		this.owner = owner;
	}
}
