package com.blackfeatherproductions.event_tracker.data_dynamic;

import java.util.Date;

import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class PopulationPlayer
{
	private Zone zone;
	private Faction faction;
	private String outfit_id; 
	
	public Date lastEvent;
}
