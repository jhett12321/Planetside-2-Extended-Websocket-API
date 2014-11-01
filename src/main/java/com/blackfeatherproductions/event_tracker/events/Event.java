package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

public interface Event
{
	/**
	 * This is where additional information can be requested by the Query Manager, or simply continue with processing the event.
	 * @param payload - The raw event payload
	 */
	public void preProcessEvent(JsonObject payload);
	
	/**
	 * This is where information retrieved by the query manager is sent to the event for merging with existing data.
	 * @param queriedData - The data retrieved from the query manager.
	 */
	public void queryCallback(JsonObject queriedData);
	
	/**
	 * This is where the completed event payload should be sent. This function updates stored character attributes, 
	 * and sends the message to the websocket server to be handled by the subscription system.
	 * @param payload - The completed event payload.
	 */
    void processEvent(JsonObject payload);
}
