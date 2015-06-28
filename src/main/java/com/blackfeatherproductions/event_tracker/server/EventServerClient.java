package com.blackfeatherproductions.event_tracker.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;

public class EventServerClient
{
    //Connection
    private final ServerWebSocket clientConnection;

    //Identification
    private final String apiKey;
    private final String name;

    //Subscriptions
    private final Map<Class<? extends Event>, JsonObject> subscriptions = new HashMap<>();
    private boolean subscribedToAll = false;

    public EventServerClient(ServerWebSocket connection, String apiKey, String name)
    {
        this.clientConnection = connection;
        this.apiKey = apiKey;
        this.name = name;

        clearSubscriptions();
    }

    public JsonObject getSubscription(Class<? extends Event> event)
    {
        return subscriptions.get(event);
    }

    public ServerWebSocket getClientConnection()
    {
        return clientConnection;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public String getName()
    {
        return name;
    }

    public boolean isSubscribedToAll()
    {
        return subscribedToAll;
    }

    public void setSubscribedToAll(boolean subscribedToAll)
    {
        this.subscribedToAll = subscribedToAll;
    }

    public void handleSubscription(String action, String eventName, JsonObject messageFilters)
    {
        Class<? extends Event> event = null;

        for (Class<? extends Event> eventType : subscriptions.keySet())
        {
            if (eventType.getAnnotation(EventInfo.class).eventName().equals(eventName))
            {
                event = eventType;
            }
        }

        if (event != null)
        {
            parseMessageFilters(messageFilters);

            JsonObject subscription = subscriptions.get(event);

            switch (action)
            {
                case "subscribe":
                    subscribe(messageFilters, subscription);
                    break;
                case "unsubscribe":
                    unsubscribe(messageFilters, subscription);
                    break;
                case "unsubcribeAll":
                    subscriptions.put(event, getBlankSubscription(event));
                    break;
                default:
                    break;
            }
        }

        else if (eventName == null && action.equals("unsubscribeAll"))
        {
            clearSubscriptions();
        }

        else if (eventName != null)
        {
            clientConnection.writeFinalTextFrame("{\"error\": \"unknownEvent\", \"message\": \"There is no Event Type by that name. Please check your syntax, and try again.\"}");
        }

        JsonObject returnSubscriptions = new JsonObject();

        JsonObject returnObject = new JsonObject();

        for (Entry<Class<? extends Event>, JsonObject> subscription : subscriptions.entrySet())
        {
            for (Entry<String, Object> element : subscription.getValue())
            {
                String property = element.getKey();

                if ((!property.equals("all") && !property.equals("worlds") && subscription.getValue().getJsonArray(property).size() > 0)
                        || (property.equals("all") && subscription.getValue().getString(property).equals("true"))
                        || (property.equals("worlds") && subscription.getValue().getJsonObject(property).size() > 0))
                {
                    returnObject.put(subscription.getKey().getAnnotation(EventInfo.class).eventName(), subscription.getValue());
                    break;
                }
            }
        }

        returnSubscriptions.put("subscriptions", returnObject);
        returnSubscriptions.put("action", action);

        clientConnection.writeFinalTextFrame(returnSubscriptions.encode());
    }

    private void subscribe(JsonObject messageFilters, JsonObject subscription)
    {
        for (String property : messageFilters.fieldNames())
        {
            if (subscription.containsKey(property))
            {
                if (property.equals("all"))
                {
                    if (messageFilters.getString(property).equals("true"))
                    {
                        subscription.put(property, "true");
                    }
                }

                else if (!property.equals("worlds") && !property.equals("zones") || (!messageFilters.containsKey("worlds") && property.equals("zones")))
                {
                    for (int i = 0; i < messageFilters.getJsonArray(property).size(); i++)
                    {
                        if (!subscription.getJsonArray(property).contains(messageFilters.getJsonArray(property).getString(i)))
                        {
                            subscription.getJsonArray(property).add(messageFilters.getJsonArray(property).getString(i));
                        }
                    }
                }

                else if (property.equals("worlds"))
                {
                    for (int i = 0; i < messageFilters.getJsonArray(property).size(); i++)
                    {
                        if (!subscription.getJsonObject(property).containsKey(messageFilters.getJsonArray(property).getString(i)))
                        {
                            JsonObject worldObject = new JsonObject();
                            worldObject.put("zones", new JsonArray());

                            subscription.getJsonObject(property).put(messageFilters.getJsonArray(property).getString(i), worldObject);
                        }

                        if (messageFilters.containsKey("zones"))
                        {
                            for (int j = 0; j < messageFilters.getJsonArray("zones").size(); j++)
                            {
                                if (!subscription.getJsonObject(property).getJsonObject(messageFilters.getJsonArray(property).getString(i)).getJsonArray("zones").contains(messageFilters.getJsonArray("zones").getString(j)))
                                {
                                    subscription.getJsonObject(property).getJsonObject(messageFilters.getJsonArray(property).getString(i)).getJsonArray("zones").add(messageFilters.getJsonArray("zones").getString(j));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void unsubscribe(JsonObject messageFilters, JsonObject subscription)
    {
        for (String property : messageFilters.fieldNames())
        {
            if (subscription.containsKey(property))
            {
                if (property.equals("all"))
                {
                    subscription.put(property, "false");
                }

                else if (!property.equals("worlds") && !property.equals("zones") || (!messageFilters.containsKey("worlds") && property.equals("zones")))
                {
                    JsonArray filteredArray = new JsonArray();

                    for (int i = 0; i < subscription.getJsonArray(property).size(); i++)
                    {
                        if (!messageFilters.getJsonArray(property).contains(subscription.getJsonArray(property).getString(i)))
                        {
                            filteredArray.add(messageFilters.getJsonArray(property).getString(i));
                        }
                    }

                    subscription.put(property, filteredArray);
                }

                else if (property.equals("worlds"))
                {
                    JsonObject filteredObject = new JsonObject();

                    for (Entry<String, Object> world : subscription.getJsonObject(property))
                    {
                        if (!messageFilters.getJsonArray(property).contains(world.getKey()))
                        {
                            JsonObject worldObject = new JsonObject();
                            JsonArray zoneArray = new JsonArray();
                            worldObject.put("zones", zoneArray);

                            filteredObject.put(world.getKey(), worldObject);
                        }

                        if (subscription.containsKey("zones"))
                        {
                            for (int i = 0; i < subscription.getJsonArray("zones").size(); i++)
                            {
                                if (!messageFilters.getJsonArray("zones").contains(subscription.getJsonArray("zones").getString(i)))
                                {
                                    filteredObject.getJsonObject(world.getKey()).getJsonArray("zones").add(subscription.getJsonArray("zones").getString(i));
                                }
                            }
                        }
                    }

                    subscription.put(property, filteredObject);
                }
            }
        }
    }

    private void clearSubscriptions()
    {
        this.subscriptions.clear();

        for (Class<? extends Event> event : EventTracker.getEventHandler().getRegisteredEvents())
        {
            //Add blank subscription to this client;
            subscriptions.put(event, getBlankSubscription(event));
        }
    }

    private JsonObject getBlankSubscription(Class<? extends Event> event)
    {
        EventInfo info = event.getAnnotation(EventInfo.class);

        JsonObject subscription = new JsonObject();
        if (!info.filters()[0].equals("no_filtering"))
        {
            //Event Specific Filters
            for (String filter : info.filters())
            {
                subscription.put(filter, new JsonArray());
            }

            //Global Filters
            subscription.put("worlds", new JsonObject());
            subscription.put("useAND", new JsonArray());
        }

        subscription.put("all", "false");
        subscription.put("environments", new JsonArray());
        subscription.put("show", new JsonArray());
        subscription.put("hide", new JsonArray());

        return subscription;
    }

    /**
     * Converts subscription filters to the correct types used internally by the
     * event tracker.
     *
     * @param messageFilters
     */
    private static void parseMessageFilters(JsonObject messageFilters)
    {
        for (Entry<String, Object> objectEntry : messageFilters.copy())
        {
            String property = objectEntry.getKey();
            Object rawData = objectEntry.getValue();

            //The all property is always a string
            if (property.equals("all"))
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

                messageFilters.remove(property);
                messageFilters.put(property, parsedProperty);
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

                messageFilters.remove(property);
                messageFilters.put(property, parsedProperty);
            }
        }
    }
}
