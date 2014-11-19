package com.blackfeatherproductions.event_tracker;

public class Utils
{
    //TODO Data verification
	public static String getWorldIDFromEndpointString(String endPointString)
	{
		String worldID = endPointString.substring(27);
		
		return worldID;
	}
	
	public static boolean isValidZone(String zoneID)
	{
		if(zoneID != null && Integer.valueOf(zoneID) < 90)
		{
			return true;
		}
		
		return false;
	}
	
	public static boolean isValidCharacter(String characterID)
	{
		if(characterID != null && characterID.length() == 19)
		{
			return true;
		}
		
		return false;
	}
}
