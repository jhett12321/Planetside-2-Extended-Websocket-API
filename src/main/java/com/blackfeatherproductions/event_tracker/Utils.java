package com.blackfeatherproductions.event_tracker;

public class Utils
{
    //TODO Census Query Processor, Data verification
	public static String getWorldIDFromEndpointString(String endPointString)
	{
		String worldID = endPointString.substring(27);
		
		return worldID;
	}
}
