package com.blackfeatherproductions.event_tracker.data;

import com.blackfeatherproductions.event_tracker.game_data.MetagameEventType;
import com.blackfeatherproductions.event_tracker.game_data.World;

public class MetagameEvent
{
	private World worldID;
	private String instanceID;
	
	private MetagameEventType metagameEventType;
	private Integer startTime;
	
	public World getWorldID()
	{
		return worldID;
	}
	
	public String getInstanceID()
	{
		return instanceID;
	}
	
	public MetagameEventType getMetagameEventType()
	{
		return metagameEventType;
	}
	
	public Integer getStartTime()
	{
		return startTime;
	}
}
