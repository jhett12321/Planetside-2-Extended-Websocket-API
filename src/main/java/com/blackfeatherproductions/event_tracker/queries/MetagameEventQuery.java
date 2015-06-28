package com.blackfeatherproductions.event_tracker.queries;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;

public class MetagameEventQuery implements Query
{
    @Override
    public void receiveData(JsonObject data, Environment environment)
    {
        if (data != null)
        {
            JsonArray eventArray = data.getJsonArray("world_event_list");

            List<String> finishedEvents = new ArrayList<>();

            for (int i = 0; i < eventArray.size(); i++)
            {
                JsonObject event = eventArray.getJsonObject(i);

                String eventState = event.getString("metagame_event_state");
                String instanceID = event.getString("instance_id");

                if (eventState.equals("137") || eventState.equals("138"))
                {
                    finishedEvents.add(instanceID);
                }
            }

            for (int i = 0; i < eventArray.size(); i++)
            {
                JsonObject event = eventArray.getJsonObject(i);

                String eventState = event.getString("metagame_event_state");
                String instanceID = event.getString("instance_id");

                if ((eventState.equals("135") || eventState.equals("136")) && !finishedEvents.contains(instanceID))
                {
                    //Process Dummy Event Message
                    JsonObject payload = new JsonObject();
                    payload.put("event_name", "MetagameEvent");
                    payload.put("instance_id", instanceID);
                    payload.put("metagame_event_id", event.getString("metagame_event_id"));
                    payload.put("metagame_event_state", eventState);
                    payload.put("timestamp", event.getString("timestamp"));
                    payload.put("world_id", event.getString("world_id"));
                    payload.put("is_dummy", "1");

                    String eventName = payload.getString("event_name");

                    EventTracker.getEventHandler().handleEvent(eventName, payload, environment);
                }
            }
        }
    }

}
