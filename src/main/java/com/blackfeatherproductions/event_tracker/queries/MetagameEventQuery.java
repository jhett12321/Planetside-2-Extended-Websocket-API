package com.blackfeatherproductions.event_tracker.queries;

import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;

public class MetagameEventQuery implements Query
{
    private final EventTracker eventTracker = EventTracker.getInstance();

    @Override
    public void receiveData(JsonObject data, Environment environment)
    {
        JsonArray eventArray = data.getArray("world_event_list");

        List<String> finishedEvents = new ArrayList<String>();

        for (int i = 0; i < eventArray.size(); i++)
        {
            JsonObject event = eventArray.get(i);

            String eventState = event.getString("metagame_event_state");
            String instanceID = event.getString("instance_id");

            if (eventState.equals("137") || eventState.equals("138"))
            {
                finishedEvents.add(instanceID);
            }
        }

        for (int i = 0; i < eventArray.size(); i++)
        {
            JsonObject event = eventArray.get(i);

            String eventState = event.getString("metagame_event_state");
            String instanceID = event.getString("instance_id");

            if ((eventState.equals("135") || eventState.equals("136")) && !finishedEvents.contains(instanceID))
            {
                //Process Dummy Event Message
                JsonObject payload = new JsonObject();
                payload.putString("event_name", "MetagameEvent");
                payload.putString("instance_id", instanceID);
                payload.putString("metagame_event_id", event.getString("metagame_event_id"));
                payload.putString("metagame_event_state", eventState);
                payload.putString("timestamp", event.getString("timestamp"));
                payload.putString("world_id", event.getString("world_id"));
                payload.putString("is_dummy", "1");

                String eventName = payload.getString("event_name");

                eventTracker.getEventHandler().handleEvent(eventName, payload, environment);
            }
        }
    }

}
