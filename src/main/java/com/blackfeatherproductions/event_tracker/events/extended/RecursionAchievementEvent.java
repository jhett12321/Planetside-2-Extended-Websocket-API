package com.blackfeatherproductions.event_tracker.events.extended;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;

@EventInfo(eventType = EventType.EVENT,
        eventName = "RecursionAchievement",
        listenedEvents = "RecursionAchievement",
        priority = EventPriority.NORMAL,
        filters =
        {
            "characters", "outfits", "factions", "achievements", "zones", "worlds"
        })
public class RecursionAchievementEvent implements Event
{
    private final EventTracker eventTracker = EventTracker.getInstance();

    private JsonObject payload;

    @Override
    public void preProcessEvent(JsonObject payload)
    {
        this.payload = payload;
        processEvent();
    }

    @Override
    public void processEvent()
    {
        //Payload
        JsonObject eventData = new JsonObject();

        eventData.putString("character_id", payload.getString("character_id"));
        eventData.putString("character_name", payload.getString("character_name"));
        eventData.putString("outfit_id", payload.getString("outfit_id"));
        eventData.putString("faction_id", payload.getString("faction_id"));
        eventData.putString("achievement_type", payload.getString("achievement_type"));
        eventData.putString("timestamp", payload.getString("timestamp"));
        eventData.putString("zone_id", payload.getString("zone_id"));
        eventData.putString("world_id", payload.getString("world_id"));

        //Filters
        JsonObject filterData = new JsonObject();

        filterData.putArray("characters", new JsonArray().addString(payload.getString("character_id")));
        filterData.putArray("outfits", new JsonArray().addString(payload.getString("outfit_id")));
        filterData.putArray("factions", new JsonArray().addString(payload.getString("faction_id")));
        filterData.putArray("achievements", new JsonArray().addString(payload.getString("achievement_type")));
        filterData.putArray("zones", new JsonArray().addString(payload.getString("zone_id")));
        filterData.putArray("worlds", new JsonArray().addString(payload.getString("world_id")));

        //Broadcast Event
        JsonObject message = new JsonObject();

        message.putObject("event_data", eventData);
        message.putObject("filter_data", filterData);

        eventTracker.getEventServer().broadcastEvent(this.getClass(), message);
    }

}
