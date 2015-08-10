package com.blackfeatherproductions.event_tracker.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ServerUtils
{
    /**
     * Converts action parameters to the correct formats for use by actions.
     *
     * @param message a REFERENCE to the raw message sent by a client.
     */
    public static void parseMessage(JsonObject message)
    {
        for (Map.Entry<String, Object> objectEntry : message.copy())
        {
            String property = objectEntry.getKey();
            Object rawData = objectEntry.getValue();

            //The all, event and action properties are always strings
            if (property.equals("all") || property.equals("event") || property.equals("action"))
            {
                String parsedProperty = "";

                if (rawData instanceof JsonArray)
                {
                    parsedProperty = ((JsonArray) rawData).getString(0);
                }
                else if (rawData instanceof JsonObject)
                {
                    parsedProperty = ((JsonObject) rawData).fieldNames().iterator().next();
                }
                else if (rawData instanceof Boolean)
                {
                    parsedProperty = ((Boolean) rawData).toString();
                }
                else
                {
                    parsedProperty = (String) rawData;
                }

                if (!parsedProperty.equals("true") && !parsedProperty.equals("false"))
                {
                    parsedProperty = "true";
                }

                message.remove(property);
                message.put(property, parsedProperty);
            }

            //Everything else should be an array.
            else
            {
                JsonArray parsedProperty = new JsonArray();

                if (rawData instanceof String)
                {
                    parsedProperty.add((String) rawData);
                }
                else if (rawData instanceof JsonObject)
                {
                    List<String> fieldNames = new ArrayList<>(((JsonObject) rawData).fieldNames());
                    parsedProperty.add(new JsonArray(fieldNames));
                }
                else if (rawData instanceof Boolean)
                {
                    parsedProperty.add(((Boolean) rawData).toString());
                }
                else
                {
                    parsedProperty = (JsonArray) rawData;
                }

                message.remove(property);
                message.put(property, parsedProperty);
            }
        }
    }
}
