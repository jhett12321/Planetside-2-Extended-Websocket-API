package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

public interface Event
{
    /**
     * This is where additional information can be requested by the Query
     * Manager, or simply continue with processing the event.
     *
     * @param payload - The raw event payload
     */
    public void preProcessEvent(JsonObject payload);

    /**
     * This is where final processing of the event should be done. All required
     * data should exist in the data manager.
     * The message to the websocket server should be sent to be handled by the
     * subscription system.
     */
    void processEvent();
}
