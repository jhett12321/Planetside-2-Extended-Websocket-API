package com.blackfeatherproductions.event_tracker.events.census;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.QueryManager;
import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;

@EventInfo(eventType = EventType.EVENT,
        eventName = "SkillAdded",
        listenedEvents = "SkillAdded",
        priority = EventPriority.NORMAL,
        filters =
        {
            "characters", "outfits", "factions", "skills", "zones", "worlds"
        })
public class SkillAddedEvent implements Event
{
    //Utils
    private final DynamicDataManager dynamicDataManager = EventTracker.instance.getDynamicDataManager();
    private final QueryManager queryManager = EventTracker.instance.getQueryManager();

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
        CharacterInfo character = dynamicDataManager.getCharacterData(characterID);
        String outfit_id = character.getOutfitID();
        Faction faction = character.getFaction();
        String skill_id = payload.getString("skill_id");
        String timestamp = payload.getString("timestamp");
        Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
        World world = World.getWorldByID(payload.getString("world_id"));

        //Event Data
        eventData.put("character_id", character.getCharacterID());
        eventData.put("character_name", character.getCharacterName());
        eventData.put("outfit_id", outfit_id);
        eventData.put("faction_id", faction.getID());
        eventData.put("skill_id", skill_id);
        eventData.put("timestamp", timestamp);
        eventData.put("zone_id", zone.getID());
        eventData.put("world_id", world.getID());

        //Filter Data
        filterData.put("characters", new JsonArray().add(character.getCharacterID()));
        filterData.put("outfits", new JsonArray().add(outfit_id));
        filterData.put("factions", new JsonArray().add(faction.getID()));
        filterData.put("skills", new JsonArray().add(skill_id));
        filterData.put("zones", new JsonArray().add(zone.getID()));
        filterData.put("worlds", new JsonArray().add(world.getID()));

        //Broadcast Event
        EventTracker.instance.getEventServer().broadcastEvent(this);
    }
}
