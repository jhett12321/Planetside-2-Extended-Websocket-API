package com.blackfeatherproductions.event_tracker.events.census;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.QueryManager;
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.Environment;

@EventInfo(eventType = EventType.EVENT,
        eventName = "ExperienceEarned",
        listenedEvents = "GainExperience",
        priority = EventPriority.NORMAL,
        filters =
        {
            "characters", "outfits", "factions", "experience_types", "loadouts", "zones", "worlds"
        })
public class ExperienceEarnedEvent implements Event
{
    //Utils
    private final EventTracker eventTracker = EventTracker.getInstance();
    private final DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();
    private final QueryManager queryManager = eventTracker.getQueryManager();

    //Raw Data
    private JsonObject payload;
    private String characterID;
    
    //Message Data
    private JsonObject eventData = new JsonObject();
    private JsonObject filterData = new JsonObject();
    private Environment environment;
    
    @Override
    public Environment getEnvironment()
    {
        return environment;
    }
    
    @Override
    public JsonObject getEventData()
    {
        return eventData;
    }

    @Override
    public JsonObject getFilterData()
    {
        return filterData;
    }

    @Override
    public void preProcessEvent(JsonObject payload, Environment environment)
    {
        this.payload = payload;
        this.environment = environment;
        
        if (payload != null)
        {
            characterID = payload.getString("character_id");

            if (dynamicDataManager.characterDataExists(characterID))
            {
                processEvent();
            }

            else
            {
                queryManager.queryCharacter(characterID, environment,  this);
            }
        }
    }

    @Override
    public void processEvent()
    {
        //Raw Data
        String amount = payload.getString("amount");
        CharacterInfo character = dynamicDataManager.getCharacterData(characterID);
        String outfit_id = character.getOutfitID();
        Faction faction = character.getFaction();
        String experience_id = payload.getString("experience_id");
        String loadout_id = payload.getString("loadout_id");
        String other_id = payload.getString("other_id");
        String timestamp = payload.getString("timestamp");
        Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
        World world = World.getWorldByID(payload.getString("world_id"));

        //Event Data
        eventData.putString("amount", amount);
        eventData.putString("character_id", character.getCharacterID());
        eventData.putString("character_name", character.getCharacterName());
        eventData.putString("outfit_id", outfit_id);
        eventData.putString("faction_id", faction.getID());
        eventData.putString("experience_id", experience_id);
        eventData.putString("loadout_id", loadout_id);
        eventData.putString("other_id", other_id);
        eventData.putString("timestamp", timestamp);
        eventData.putString("zone_id", zone.getID());
        eventData.putString("world_id", world.getID());

        //Filter Data
        filterData.putArray("characters", new JsonArray().addString(character.getCharacterID()));
        filterData.putArray("outfits", new JsonArray().addString(outfit_id));
        filterData.putArray("factions", new JsonArray().addString(faction.getID()));
        filterData.putArray("experience_types", new JsonArray().addString(experience_id));
        filterData.putArray("loadouts", new JsonArray().addString(loadout_id));
        filterData.putArray("zones", new JsonArray().addString(zone.getID()));
        filterData.putArray("worlds", new JsonArray().addString(world.getID()));

        //Broadcast Event
        eventTracker.getEventServer().broadcastEvent(this);
    }
}
