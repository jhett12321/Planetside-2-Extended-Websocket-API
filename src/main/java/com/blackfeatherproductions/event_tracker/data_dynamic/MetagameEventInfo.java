package com.blackfeatherproductions.event_tracker.data_dynamic;

import com.blackfeatherproductions.event_tracker.data_static.MetagameEventType;

public class MetagameEventInfo
{
	private MetagameEventType type;
	private String startTime;
	private String endTime;
	private String instanceID;
	
	public MetagameEventInfo(String instanceID, MetagameEventType type, String startTime, String endTime)
	{
		this.instanceID = instanceID;
		this.type = type;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	/**
	 * @return the instanceID
	 */
	public String getInstanceID()
	{
		return instanceID;
	}
	
	/**
	 * @return the type
	 */
	public MetagameEventType getType()
	{
		return type;
	}

	/**
	 * @return the startTime
	 */
	public String getStartTime()
	{
		return startTime;
	}

	/**
	 * @return the endTime
	 */
	public String getEndTime()
	{
		return endTime;
	}
}
