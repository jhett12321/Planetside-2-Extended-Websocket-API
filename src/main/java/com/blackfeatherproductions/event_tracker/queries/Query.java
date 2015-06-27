package com.blackfeatherproductions.event_tracker.queries;

import com.blackfeatherproductions.event_tracker.Environment;

import io.vertx.core.json.JsonObject;

public interface Query
{
    public void receiveData(JsonObject data, Environment environment);
}
