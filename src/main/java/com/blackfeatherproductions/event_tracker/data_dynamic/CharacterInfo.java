package com.blackfeatherproductions.event_tracker.data_dynamic;

import org.vertx.java.core.Handler;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class CharacterInfo
{
    private final EventTracker eventTracker = EventTracker.getInstance();

    private final String characterID;
    private final String characterName;
    private final String outfitID;
    private final Faction faction;
    private final Zone zone;
    private final World world;
    private final boolean online;

    public CharacterInfo(final String characterID, String characterName, String factionID, String outfitID, String zoneID, String worldID, boolean online)
    {
        this.characterID = characterID;
        this.characterName = characterName;
        this.faction = Faction.getFactionByID(factionID);
        this.outfitID = outfitID;
        this.zone = Zone.getZoneByID(zoneID);
        this.world = World.getWorldByID(worldID);
        this.online = online;

        eventTracker.getVertx().setTimer(60000, new Handler<Long>()
        {
            @Override
            public void handle(Long timerID)
            {
                eventTracker.getDynamicDataManager().removeCharacter(characterID);
            }
        });
    }

    public Faction getFaction()
    {
        return faction;
    }

    public String getOutfitID()
    {
        return outfitID; //TODO 1.3 map outfits for translator
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

    public boolean isOnline()
    {
        return online;
    }
}
