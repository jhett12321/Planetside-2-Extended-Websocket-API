package com.blackfeatherproductions.EventTracker.events;

import org.vertx.java.core.json.JsonObject;


@EventInfo(eventNames = "DirectiveCompleted")
public class DirectiveCompletedEvent implements Event
{
    @Override
    public void processEvent(JsonObject payload)
    {
     // TODO Auto-generated method stub
    }

    @Override
    public JsonObject getBlankSubscription()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
