package com.blackfeatherproductions.EventTracker.events;

import org.vertx.java.core.json.JsonObject;

@EventInfo(eventNames = "VehicleDestroy")
public class VehicleDestroyEvent implements Event
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
