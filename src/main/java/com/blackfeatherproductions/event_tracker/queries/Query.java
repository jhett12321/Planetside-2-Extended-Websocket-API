package com.blackfeatherproductions.event_tracker.queries;

import org.vertx.java.core.json.JsonObject;

public interface Query
{
    public void receiveData(JsonObject data);
}
