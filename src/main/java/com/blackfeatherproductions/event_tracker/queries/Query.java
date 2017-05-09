package com.blackfeatherproductions.event_tracker.queries;

import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_static.Environment;

public interface Query
{
    void receiveData(JsonObject data, Environment environment);
}
