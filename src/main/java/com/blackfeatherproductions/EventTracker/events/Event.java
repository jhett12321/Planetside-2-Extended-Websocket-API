package com.blackfeatherproductions.EventTracker.events;

import org.vertx.java.core.json.JsonObject;

public interface Event
{
    public void processEvent(JsonObject payload);
    public JsonObject getBlankSubscription();
}
