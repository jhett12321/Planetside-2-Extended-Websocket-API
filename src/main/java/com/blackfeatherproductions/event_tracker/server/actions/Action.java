package com.blackfeatherproductions.event_tracker.server.actions;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

public interface Action
{
    public void processAction(ServerWebSocket clientConnection, JsonObject actionData);
}
