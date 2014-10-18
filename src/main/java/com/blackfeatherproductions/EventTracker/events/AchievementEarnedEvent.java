package com.blackfeatherproductions.EventTracker.events;

import org.vertx.java.core.json.JsonObject;


@EventInfo(eventNames = "AchievementEarned")
public class AchievementEarnedEvent implements Event
{
    @Override
    public void processEvent(JsonObject payload)
    {
     // TODO Auto-generated method stub
    }

    @Override
    public JsonObject getBlankSubscription()
    {
        return new JsonObject("{all: \"false\", worlds: {}, useAND: [], show: [], hide: [], characters: [], outfits: [], factions: [], achievements: [], zones: []}");
    }
}
