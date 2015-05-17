package com.blackfeatherproductions.event_tracker.events.census;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventName = "DirectiveCompleted",
        listenedEvents = "DirectiveCompleted",
        priority = EventPriority.NORMAL,
        filters =
        {
            "characters", "outfits", "factions", "directives", "zones", "worlds"
        })
public class DirectiveCompletedEvent implements Event
{
    private final EventTracker eventTracker = EventTracker.getInstance();
    private final DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();

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

        String directive_id = payload.getString("directive_id");

        String timestamp = payload.getString("timestamp");
        Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
        World world = World.getWorldByID(payload.getString("world_id"));

        //Payload
        JsonObject eventData = new JsonObject();

        eventData.putString("character_id", character.getCharacterID());
        eventData.putString("character_name", character.getCharacterName());
        eventData.putString("outfit_id", outfit_id);
        eventData.putString("faction_id", faction.getID());
        eventData.putString("directive_id", directive_id);
        eventData.putString("timestamp", timestamp);
        eventData.putString("zone_id", zone.getID());
        eventData.putString("world_id", world.getID());

        //Filters
        JsonObject filterData = new JsonObject();

        filterData.putArray("characters", new JsonArray().addString(character.getCharacterID()));
        filterData.putArray("outfits", new JsonArray().addString(outfit_id));
        filterData.putArray("factions", new JsonArray().addString(faction.getID()));
        filterData.putArray("directives", new JsonArray().addString(directive_id));
        filterData.putArray("zones", new JsonArray().addString(zone.getID()));
        filterData.putArray("worlds", new JsonArray().addString(world.getID()));

        //Broadcast Event
        JsonObject message = new JsonObject();

        message.putObject("event_data", eventData);
        message.putObject("filter_data", filterData);

        eventTracker.getEventServer().broadcastEvent(this.getClass(), message);
        eventTracker.countProcessedEvent();
    }
}
