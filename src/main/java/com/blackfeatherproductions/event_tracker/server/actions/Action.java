package com.blackfeatherproductions.event_tracker.server.actions;

import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

public interface Action
{
	public void processAction(ServerWebSocket clientConnection, JsonObject actionData);
}
