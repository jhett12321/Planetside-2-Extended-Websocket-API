package com.blackfeatherproductions.event_tracker;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data.Faction;
import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.Zone;
import com.blackfeatherproductions.event_tracker.data.dynamic.FacilityInfo;
import com.blackfeatherproductions.event_tracker.data.dynamic.ZoneInfo;

public class Utils
{
	private static DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
	
	public static String getWorldIDFromEndpointString(String endPointString)
	{
		String worldID = endPointString.substring(27);
		
		return worldID;
	}
	
	public static boolean isValidPayload(JsonObject payload)
	{
		if(payload.containsField("zone_id"))
		{
			if(!isValidZone(payload.getString("zone_id")))
			{
				return false;
			}
		}
		
		if(payload.containsField("attacker_character_id"))
		{
			if(!isValidCharacter(payload.getString("attacker_character_id")) && !isValidCharacter(payload.getString("character_id")))
			{
				return false;
			}
		}
		
		else if(payload.containsField("character_id"))
		{
			if(!isValidCharacter(payload.getString("character_id")))
			{
				return false;
			}
		}
		
		return true;
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
	
	//Calculates Territory Control for the given zone
	public static JsonObject calculateTerritoryControl(World world, Zone zone)
	{
		float totalRegions = 0;
		float facilitiesVS = 0;
		float facilitiesNC = 0;
		float facilitiesTR = 0;
		
		for(FacilityInfo facility : dynamicDataManager.getWorldData(world).getZoneInfo(zone).getFacilities().values())
		{
			totalRegions++;
			
			if(facility.getOwner() == Faction.VS)
			{
				facilitiesVS++;
			}
			else if(facility.getOwner() == Faction.NC)
			{
				facilitiesNC++;
			}
			else if(facility.getOwner() == Faction.TR)
			{
				facilitiesTR++;
			}
		}
		
		Float controlVS = 0f;
		Float controlNC = 0f;
		Float controlTR = 0f;
		
		if(totalRegions > 0)
		{
			controlVS = (float) Math.floor(facilitiesVS / totalRegions * 100);
			controlNC = (float) Math.floor(facilitiesNC / totalRegions * 100);
			controlTR = (float) Math.floor(facilitiesTR / totalRegions * 100);
		}
		
		float majorityControl = controlVS;
		Faction majorityController = Faction.VS;
		
		if(controlNC > majorityControl)
		{
			majorityControl = controlNC;
			majorityController = Faction.NC;
		}
		else if(controlNC == majorityControl)
		{
			majorityController = null;
		}
		
		if(controlTR > majorityControl)
		{
			majorityControl = controlTR;
			majorityController = Faction.TR;
		}
		else if(controlTR == majorityControl)
		{
			majorityController = null;
		}
		
		JsonObject controlInfo = new JsonObject();
		
		controlInfo.putString("control_vs", controlVS.toString());
		controlInfo.putString("control_nc", controlNC.toString());
		controlInfo.putString("control_tr", controlTR.toString());
		controlInfo.putString("majority_controller", majorityController.getId());
		
		return controlInfo;
	}
	
	//Calculates Territory Control for the given world
	public static JsonObject calculateTerritoryControl(World world)
	{
		float totalRegions = 0;
		float facilitiesVS = 0;
		float facilitiesNC = 0;
		float facilitiesTR = 0;
		
		for(ZoneInfo zone : dynamicDataManager.getWorldData(world).getZones().values())
		{
			for(FacilityInfo facility : zone.getFacilities().values())
			{
				totalRegions++;
				
				if(facility.getOwner() == Faction.VS)
				{
					facilitiesVS++;
				}
				else if(facility.getOwner() == Faction.NC)
				{
					facilitiesNC++;
				}
				else if(facility.getOwner() == Faction.TR)
				{
					facilitiesTR++;
				}
			}
		}
		
		Float controlVS = 0f;
		Float controlNC = 0f;
		Float controlTR = 0f;
		
		if(totalRegions > 0)
		{
			controlVS = (float) Math.floor(facilitiesVS / totalRegions * 100);
			controlNC = (float) Math.floor(facilitiesNC / totalRegions * 100);
			controlTR = (float) Math.floor(facilitiesTR / totalRegions * 100);
		}
		
		float majorityControl = controlVS;
		Faction majorityController = Faction.VS;
		
		if(controlNC > majorityControl)
		{
			majorityControl = controlNC;
			majorityController = Faction.NC;
		}
		else if(controlNC == majorityControl)
		{
			majorityController = null;
		}
		
		if(controlTR > majorityControl)
		{
			majorityControl = controlTR;
			majorityController = Faction.TR;
		}
		else if(controlTR == majorityControl)
		{
			majorityController = null;
		}
		
		JsonObject controlInfo = new JsonObject();
		
		controlInfo.putString("control_vs", controlVS.toString());
		controlInfo.putString("control_nc", controlNC.toString());
		controlInfo.putString("control_tr", controlTR.toString());
		controlInfo.putString("majority_controller", majorityController.getId());
		
		return controlInfo;
	}
}
