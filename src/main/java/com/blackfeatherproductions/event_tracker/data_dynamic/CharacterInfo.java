package com.blackfeatherproductions.event_tracker.data_dynamic;

import java.util.Date;

import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class CharacterInfo
{
    private String characterID;
    private String characterName;
    private String outfitID;
    private Faction faction;
    private Zone zone;
    private World world;
    private boolean online;
    private Date updateTime;


    public void setCharacterData(final String characterID, String characterName, String factionID, String outfitID, String zoneID, String worldID, boolean online)
    {
        this.characterID = characterID;
        this.characterName = characterName;
        this.faction = Faction.getFactionByID(factionID);
        this.outfitID = outfitID;
        this.zone = Zone.getZoneByID(zoneID);
        this.world = World.getWorldByID(worldID);
        this.online = online;
        this.updateTime = new Date();
    }

    public Faction getFaction()
    {
        return faction;
    }

    public String getOutfitID()
    {
        return outfitID; //TODO FUTURE map outfits for translator
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

    public World getWorld()
    {
        return world;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void updateTime()
    {
        updateTime = new Date();
    }

    public boolean isOnline()
    {
        return online;
    }

    public void setCharacterName(String characterName)
    {
        this.characterName = characterName;
    }

    public void setOutfitID(String outfitID)
    {
        this.outfitID = outfitID;
    }

    public void setFaction(Faction faction)
    {
        this.faction = faction;
    }

    public void setZone(Zone zone)
    {
        this.zone = zone;
    }

    public void setWorld(World world)
    {
        this.world = world;
    }

    public void setOnline(boolean online)
    {
        this.online = online;
    }
}
