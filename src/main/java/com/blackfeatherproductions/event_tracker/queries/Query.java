package com.blackfeatherproductions.event_tracker.queries;

import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.Environment;

public interface Query
{
    public void receiveData(JsonObject data, Environment environment);
}
