package com.blackfeatherproductions.event_tracker.queries.static_data;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.data_static.MetagameEventType;
import com.blackfeatherproductions.event_tracker.queries.Query;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MetagameEventTypeQuery implements Query
{
    @Override
    public void receiveData(JsonObject data, Environment environment)
    {
        JsonArray metagame_event_list = data.getJsonArray("metagame_event_list");

        for (int i = 0; i < metagame_event_list.size(); i++)
        {
            JsonObject event_type = metagame_event_list.getJsonObject(i);
            
            String metagame_event_id = event_type.getString("metagame_event_id");
            String name = event_type.getJsonObject("name").getString("en");
            String desc = event_type.getJsonObject("description").getString("en");
            String type = event_type.getString("type");

            if(!MetagameEventType.metagameEventTypes.containsKey(metagame_event_id))
            {
                EventTracker.instance.getLogger().info("Registering Metagame event ID " + metagame_event_id + " (" + name + ")");
                MetagameEventType.metagameEventTypes.put(metagame_event_id, new MetagameEventType(metagame_event_id, name, desc, type));
            }
        }
    }
}
