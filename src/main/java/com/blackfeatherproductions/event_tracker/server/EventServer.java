package com.blackfeatherproductions.event_tracker.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;

import com.blackfeatherproductions.event_tracker.Config;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.MavenInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.server.actions.Action;
import com.blackfeatherproductions.event_tracker.server.actions.ActionInfo;
import com.blackfeatherproductions.event_tracker.server.actions.ActiveAlerts;
import com.blackfeatherproductions.event_tracker.server.actions.FacilityStatus;
import com.blackfeatherproductions.event_tracker.server.actions.ActiveMetagameEvents;
import com.blackfeatherproductions.event_tracker.server.actions.WorldStatus;
import com.blackfeatherproductions.event_tracker.server.actions.ZoneStatus;
import com.blackfeatherproductions.event_tracker.utils.ServerUtils;

public class EventServer
{
    private final Config config;

    //API Key Database
    private final String dbUrl;

    //Actions
    private final Map<ActionInfo, Class<? extends Action>> actions = new LinkedHashMap<>();

    //Client Info
    private final Map<ServerWebSocket, EventServerClient> clientConnections = new ConcurrentHashMap<>();

    public EventServer()
    {
        config = EventTracker.instance.getConfig();
        
        //API Key Database
        dbUrl = "jdbc:mysql://" + config.getDbHost() + "/" + config.getDbName();

        //Client Actions
        registerActions();
        
        //Websocket Server
        HttpServer server = EventTracker.instance.getVertx().createHttpServer(new HttpServerOptions().setPort(config.getServerPort()));
        
        server.websocketHandler(clientConnection ->
        {
            Pump.pump(clientConnection, clientConnection);
            
            Map<String, String> queryPairs = new LinkedHashMap<>();

            String query = clientConnection.query();
            String[] pairs = query.split("&");
            for (String pair : pairs)
            {
                int idx = pair.indexOf('=');
                try
                {
                    queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }
            }

            final String apiKey = queryPairs.get("apikey");
            final String apiName;
            
            if(config.isAuthEnabled())
            {
                apiName = verifyAPIKey(apiKey);
            }
            else
            {
                apiName = "AUTH DISABLED";
            }
            
            if (apiName != null)
            {
                clientConnection.closeHandler(v ->
                {
                    EventTracker.instance.getLogger().info("Client " + apiName + " Disconnected. (Connection Closed) API Key: " + apiKey);
                    
                    EventServerClient client = clientConnections.remove(clientConnection);
                });
                
                clientConnection.endHandler(v ->
                {
                    EventTracker.instance.getLogger().info("Client " + apiName + " Disconnected. (Connection Ended) API Key: " + apiKey);
                    
                    EventServerClient client = clientConnections.remove(clientConnection);
                });
                
                clientConnection.exceptionHandler(e ->
                {
                    EventTracker.instance.getLogger().info("Client " + apiName + " Disconnected. (Connection Exception) API Key: " + apiKey);
                    
                    EventServerClient client = clientConnections.remove(clientConnection);
                });

                clientConnection.handler(data ->
                {
                    JsonObject message = null;

                    try
                    {
                        message = new JsonObject(data.toString());
                    }
                    catch (Exception e)
                    {
                        clientConnection.writeFinalTextFrame("{\"error\": \"BADJSON\", \"message\": \"You have supplied an invalid JSON string. Please check your syntax.\"}");
                    }

                    if (message != null)
                    {
                        EventTracker.instance.getLogger().info("Client " + apiName + " Sent Valid JSON Message.");
                        EventTracker.instance.getLogger().info(message.encodePrettily());
                        handleClientMessage(clientConnection, message);
                    }
                });
                
                clientConnections.put(clientConnection, new EventServerClient(clientConnection, apiKey, apiName));
                EventTracker.instance.getLogger().info("Client " + apiName + " Connected! API Key: " + apiKey);

                //Send Connection Confirmed Message
                JsonObject connectMessage = new JsonObject();
                connectMessage.put("service", "ps2_events");
                connectMessage.put("version", MavenInfo.getVersion());
                connectMessage.put("websocket_event", "connectionStateChange");
                connectMessage.put("online", "true");

                clientConnection.writeFinalTextFrame(connectMessage.encode());

                //Send Service Status Messages
                for (Entry<World, WorldInfo> worldEntry : EventTracker.instance.getDynamicDataManager().getAllWorldInfo().entrySet())
                {
                    JsonObject serviceMessage = new JsonObject();

                    JsonObject payload = new JsonObject();
                    payload.put("online", worldEntry.getValue().isOnline() ? "1" : "0");
                    payload.put("world_id", worldEntry.getKey().getID());

                    serviceMessage.put("payload", payload);
                    serviceMessage.put("event_type", "ServiceStateChange");
                    clientConnection.writeFinalTextFrame(serviceMessage.encode());
                }
            }
            else
            {
                clientConnection.reject();
            }
        }).listen();
    }

    private String verifyAPIKey(String apiKey)
    {
        if (apiKey != null && !apiKey.equals(""))
        {
            Connection dbConnection = null;

            try
            {
                Class.forName("com.mysql.jdbc.Driver");

                dbConnection = DriverManager.getConnection(dbUrl, config.getDbUser(), config.getDbPassword());

                PreparedStatement query = dbConnection.prepareStatement("SELECT * FROM `api_keys` WHERE api_key = ? AND enabled = 1");
                query.setString(1, apiKey);
                ResultSet resultSet = query.executeQuery();

                //Check if API Key exists
                String apiName = null;
                if (resultSet.next())
                {
                    apiName = resultSet.getString("name");
                }

                query.close();
                
                if(apiName != null)
                {
                    query = dbConnection.prepareStatement("UPDATE `api_keys` SET last_auth = ? WHERE api_key = ?");
                    query.setInt(1, (int) (new Date().getTime() / 1000));
                    query.setString(2, apiKey);
                    
                    query.executeUpdate();
                    
                    query.close();
                }
                
                dbConnection.close();

                return apiName;
            }

            catch (ClassNotFoundException | SQLException e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    private void registerActions()
    {
        registerAction(ActiveMetagameEvents.class);
        registerAction(WorldStatus.class);
        
        //TODO Deprecated Actions
        registerAction(ActiveAlerts.class);
        registerAction(ZoneStatus.class);
        registerAction(FacilityStatus.class);
    }

    private void handleClientMessage(ServerWebSocket clientConnection, JsonObject message)
    {
        //Parse and validate this message. This also fixes some syntax issues, e.g. using JStrings instead of JArrays
        ServerUtils.parseMessage(message);
        
        String eventType = message.getString("event");
        String action = message.getString("action");

        message.remove("event");
        message.remove("action");

        if (!action.matches("subscribe|unsubscribe|unsubscribeall"))
        {
            handleAction(clientConnection, action, message);
        }

        else if (action.matches("subscribe|unsubscribe|unsubscribeall"))
        {
            clientConnections.get(clientConnection).handleSubscription(action, eventType, message);
        }

        else
        {
            clientConnection.writeFinalTextFrame("{\"error\": \"unknownAction\", \"message\": \"There is no Action by that name. Please check your syntax, and try again.\"}");
        }
    }

    private void handleAction(ServerWebSocket clientConnection, String actionName, JsonObject actionData)
    {
        for (Entry<ActionInfo, Class<? extends Action>> entry : actions.entrySet())
        {
            if (actionName.matches(entry.getKey().actionNames()))
            {
                try
                {
                    Action action = entry.getValue().newInstance();

                    action.processAction(clientConnection, actionData);
                }
                catch (InstantiationException | IllegalAccessException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void registerAction(Class<? extends Action> action)
    {
        ActionInfo info = action.getAnnotation(ActionInfo.class);
        if (info == null)
        {
            EventTracker.instance.getLogger().warn("Implementing Action Class: " + action.getName() + " is missing a required annotation.");
            return;
        }

        actions.put(info, action);
    }

    public void broadcastEvent(Event event)
    {
        Class<? extends Event> eventClass = event.getClass();
        JsonObject messageToSend = new JsonObject();

        JsonObject eventFilterData = event.getFilterData();
        JsonObject eventData = event.getEventData();
        
        //Put the event's environment into the filter data
        eventFilterData.put("environments", new JsonArray().add(event.getEnvironment().fieldName));

        messageToSend.put("payload", eventData);
        messageToSend.put("event_type", eventClass.getAnnotation(EventInfo.class).eventName());
        messageToSend.put("environment", event.getEnvironment().fieldName);

        Iterator<Entry<ServerWebSocket, EventServerClient>> connectionIter = clientConnections.entrySet().iterator();
        
        while(connectionIter.hasNext())
        {
            Entry<ServerWebSocket, EventServerClient> connection = connectionIter.next();
            EventType eventType = eventClass.getAnnotation(EventInfo.class).eventType();

            // Don't send Listener events to clients.
            if(eventType.equals(EventType.LISTENER))
            {
                return;
            }

            // This is a service event. This will always be sent.
            if (eventType.equals(EventType.SERVICE))
            {
                connection.getKey().writeFinalTextFrame(messageToSend.encode());
                return;
            }

            // Get Subscription for provided event
            JsonObject subscription = connection.getValue().getSubscription(eventClass);

            // Check to see if we should send this event.
            if(!checkCanSendEvent(subscription, eventFilterData))
            {
                return;
            }

            // Filter out all fields that do not match the fields requested by the client.
            if (subscription.getJsonArray("show").size() > 0)
            {
                JsonObject filteredPayload = new JsonObject();

                for (String field : eventData.fieldNames())
                {
                    if (subscription.getJsonArray("show").contains(field))
                    {
                        filteredPayload.put(field, eventData.getString(field));
                    }
                }

                messageToSend.put("payload", filteredPayload);
            }

            // Hide any additional fields requested by the client.
            if (subscription.getJsonArray("hide").size() > 0)
            {
                JsonObject filteredPayload = messageToSend.getJsonObject("payload");

                for (String field : eventData.copy().fieldNames())
                {
                    if (subscription.getJsonArray("hide").contains(field))
                    {
                        filteredPayload.remove(field);
                    }
                }

                messageToSend.put("payload", filteredPayload);
            }

            // Send the event.
            connection.getKey().writeFinalTextFrame(messageToSend.encode());
        }
    }

    private boolean checkCanSendEvent(JsonObject subscription, JsonObject eventFilterData)
    {
        // If we are sending all events, no filtering is required.
        if (subscription.getString("all").equals("true"))
        {
            return true;
        }

        // This will be set when an event filter is not empty.
        boolean filteredEvent = false;

        for (String subscriptionProperty : subscription.fieldNames())
        {
            if (!subscriptionProperty.equals("all") && !subscriptionProperty.equals("useAND") && !subscriptionProperty.equals("show") && !subscriptionProperty.equals("hide"))
            {
                JsonArray filterData = eventFilterData.getJsonArray(subscriptionProperty);

                // The worlds filter is special in the fact that a world + zone can be specified.
                // Separate logic is done to handle this behaviour.
                if (subscriptionProperty.equals("worlds"))
                {
                    JsonObject subscriptionValue = subscription.getJsonObject(subscriptionProperty);

                    // This filter is empty.
                    if (subscriptionValue.size() == 0)
                    {
                         continue;
                    }

                    // This event has at least 1 filter.
                    filteredEvent = true;

                    // The filter does not contain any matching worlds.
                    if (!subscriptionValue.containsKey(filterData.getString(0)))
                    {
                        return false;
                    }

                    JsonArray subscriptionZoneData = subscriptionValue.getJsonObject(filterData.getString(0)).getJsonArray("zones");
                    JsonArray zoneData = eventFilterData.getJsonArray("zones");

                    // If no zones have been defined, or if we contain a matching zone, go to the next filter.
                    if (subscriptionZoneData == null || subscriptionZoneData.isEmpty() || subscriptionZoneData.contains(zoneData.getString(0)))
                    {
                        continue;
                    }

                    return false;
                }

                // For all other filters...
                JsonArray subscriptionValue = subscription.getJsonArray(subscriptionProperty);

                // This filter is empty. Go to the next.
                if (subscriptionValue.size() == 0)
                {
                    continue;
                }

                // Some events specify multiple dynamic values (characters, outfits).
                // These values are checked with AND/OR logic as specified by the client.

                // For AND, we immediately return false if one value does not match.
                if (subscription.getJsonArray("useAND").contains(subscriptionProperty))
                {
                    for (int i = 0; i < filterData.size(); i++)
                    {
                        if (!subscriptionValue.contains(filterData.getString(i)))
                        {
                            return false;
                        }
                    }

                    continue;
                }

                // For OR, we only return false if nothing matched.
                else
                {
                    boolean somethingMatched = false;
                    for (int i = 0; i < filterData.size(); i++)
                    {
                        if (subscriptionValue.contains(filterData.getString(i)))
                        {
                            somethingMatched = true;
                            break;
                        }
                    }

                    if(!somethingMatched)
                    {
                        return false;
                    }
                }
            }
        }

        if(filteredEvent)
        {
            return true;
        }

        return false;
    }
}
