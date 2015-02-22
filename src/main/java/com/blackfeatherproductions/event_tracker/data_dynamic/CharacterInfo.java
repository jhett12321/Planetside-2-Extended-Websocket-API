package com.blackfeatherproductions.event_tracker.data_dynamic;

import org.vertx.java.core.Handler;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class CharacterInfo
{
	private String characterID;
	private String characterName;
	private String outfitID;
	private Faction faction;
	private Zone zone;

	public CharacterInfo(final String characterID, String characterName, String factionID, String outfitID, String zoneID)
	{
		this.characterID = characterID;
		this.characterName = characterName;
		this.faction = Faction.getFactionByID(factionID);
		this.outfitID = outfitID;
		this.zone = Zone.getZoneByID(zoneID);
		
        EventTracker.getInstance().getVertx().setTimer(60000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	EventTracker.getInstance().getDynamicDataManager().removeCharacter(characterID);
            }
        });
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

	public String getCharacterName()
	{
		return characterName;
	}

	public Zone getZone()
	{
		return zone;
	}
}
