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
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventType = EventType.EVENT,
        eventName = "AchievementEarned",
        listenedEvents = "AchievementEarned",
        priority = EventPriority.NORMAL,
        filters =
        {
            "characters", "outfits", "factions", "achievements", "zones", "worlds"
        })
public class AchievementEarnedEvent implements Event
{
    private final EventTracker eventTracker = EventTracker.getInstance();
    private final DynamicDataManager dynamicDataManager = eventTracker.getDynamicDataManager();

    private JsonObject payload;

    private String characterID;

    @Override
    public void preProcessEvent(JsonObject payload)
    {
        this.payload = payload;
        if (payload != null)
        {
            characterID = payload.getString("character_id");

            if (dynamicDataManager.characterDataExists(characterID))
            {
                processEvent();
            }

            else
            {
                new CharacterQuery(characterID, this);
            }
        }
    }

    @Override
    public void processEvent()
    {
        //Data
        CharacterInfo character = dynamicDataManager.getCharacterData(characterID);
        String outfit_id = character.getOutfitID();
        Faction faction = character.getFaction();
        String achievement_id = payload.getString("achievement_id");
        String timestamp = payload.getString("timestamp");
        Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
        World world = World.getWorldByID(payload.getString("world_id"));

        //Payload
        JsonObject eventData = new JsonObject();

        eventData.putString("character_id", character.getCharacterID());
        eventData.putString("character_name", character.getCharacterName());
        eventData.putString("outfit_id", outfit_id);
        eventData.putString("faction_id", faction.getID());
        eventData.putString("achievement_id", achievement_id);
        eventData.putString("timestamp", timestamp);
        eventData.putString("zone_id", zone.getID());
        eventData.putString("world_id", world.getID());

        //Filters
        JsonObject filterData = new JsonObject();

        filterData.putArray("characters", new JsonArray().addString(character.getCharacterID()));
        filterData.putArray("outfits", new JsonArray().addString(outfit_id));
        filterData.putArray("factions", new JsonArray().addString(faction.getID()));
        filterData.putArray("achievements", new JsonArray().addString(achievement_id));
        filterData.putArray("zones", new JsonArray().addString(zone.getID()));
        filterData.putArray("worlds", new JsonArray().addString(world.getID()));

        //Broadcast Event
        JsonObject message = new JsonObject();

        message.putObject("event_data", eventData);
        message.putObject("filter_data", filterData);

        eventTracker.getEventServer().broadcastEvent(this.getClass(), message);
    }
}
