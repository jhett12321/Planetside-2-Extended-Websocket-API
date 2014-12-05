package com.blackfeatherproductions.event_tracker.data.dynamic;

import java.util.Date;

import com.blackfeatherproductions.event_tracker.data.Faction;

public class CharacterInfo
{
	private String characterID;
	private String outfitID;
	private Faction faction;
	
	private Date creationTime;
	
	public Faction getFaction()
	{
		return faction;
	}
	
	public String getOutfitID()
	{
		return outfitID; //TODO map outfits for translator
	}
	
	public String getCharacterID()
	{
		return characterID;
	}

	public CharacterInfo(String characterID, String factionID, String outfitID)
	{
		this.characterID = characterID;
		this.faction = Faction.getFactionByID(factionID);
		this.outfitID = outfitID;
		
		this.creationTime = new Date();
	}
}
