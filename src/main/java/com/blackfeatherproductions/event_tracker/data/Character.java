package com.blackfeatherproductions.event_tracker.data;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.game_data.Faction;

public class Character
{
	private String characterID;
	private String outfitID;
	private Faction faction;
	
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

	public Character(String characterID, String factionID, String outfitID)
	{
		this.characterID = characterID;
		this.faction = EventTracker.getInstance().getGameData().getFactionByID(factionID);
		this.outfitID = outfitID;
	}
}
