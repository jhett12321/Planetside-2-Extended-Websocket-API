package com.blackfeatherproductions.event_tracker.events.census;

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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();
    private final QueryManager queryManager = EventTracker.getQueryManager();

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
                queryManager.queryCharacter(characterID, environment, this);
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
        eventData.put("amount", amount);
        eventData.put("character_id", character.getCharacterID());
        eventData.put("character_name", character.getCharacterName());
        eventData.put("outfit_id", outfit_id);
        eventData.put("faction_id", faction.getID());
        eventData.put("experience_id", experience_id);
        eventData.put("loadout_id", loadout_id);
        eventData.put("other_id", other_id);
        eventData.put("timestamp", timestamp);
        eventData.put("zone_id", zone.getID());
        eventData.put("world_id", world.getID());

        //Filter Data
        filterData.put("characters", new JsonArray().add(character.getCharacterID()));
        filterData.put("outfits", new JsonArray().add(outfit_id));
        filterData.put("factions", new JsonArray().add(faction.getID()));
        filterData.put("experience_types", new JsonArray().add(experience_id));
        filterData.put("loadouts", new JsonArray().add(loadout_id));
        filterData.put("zones", new JsonArray().add(zone.getID()));
        filterData.put("worlds", new JsonArray().add(world.getID()));

        //Broadcast Event
        EventTracker.getEventServer().broadcastEvent(this);
    }
}
