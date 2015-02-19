package com.blackfeatherproductions.event_tracker.data_dynamic;

import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class CharacterInfo
{
	private String characterID;
	private String outfitID;
	private Faction faction;
	private Zone zone;

	public CharacterInfo(String characterID, String factionID, String outfitID, String zoneID)
	{
		this.characterID = characterID;
		this.faction = Faction.getFactionByID(factionID);
		this.outfitID = outfitID;
		this.zone = Zone.getZoneByID(zoneID);
	}
	
	public Faction getFaction()
	{
		return faction;
	}
	
	public String getOutfitID()
	{
		return outfitID; //TODO 1.1 map outfits for translator
	}
	
	public String getCharacterID()
	{
		return characterID;
	}

	public Zone getZone()
	{
		return zone;
	}
}
