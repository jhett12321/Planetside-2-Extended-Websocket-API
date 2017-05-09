package com.blackfeatherproductions.event_tracker.events;

import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_static.Environment;

public interface Event
{
    /**
     * This is where additional information can be requested by the Query
     * Manager, or simply continue with processing the event.
     *
     * @param payload - The raw event payload
     * @param environment - The environment (PC, PS4) for this event.
     */
    void preProcessEvent(JsonObject payload, Environment environment);

    /**
     * This is where final processing of the event should be done. All required
     * data should exist in the data manager. The message to the websocket
     * server should be sent to be handled by the subscription system.
     */
    void processEvent();

    /**
     *
     * @return The environment (PC, PS4) of this event.
     */
    Environment getEnvironment();

    /**
     *
     * @return A list of JsonArrays containing completed filter data as
     * specified by the EventInfo annotation.
     */
    JsonObject getFilterData();

    /**
     *
     * @return The completed event payload of this event.
     */
    JsonObject getEventData();
}
