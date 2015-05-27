package com.blackfeatherproductions.event_tracker.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

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
import com.blackfeatherproductions.event_tracker.server.actions.ZoneStatus;

//TODO Make bad JSON more verbose for clients, and not throw exceptions.
//TODO Refactor "useAND" to "exclusive mode"
//TODO Implement subscription mode
//4am thoughts:
//4:14 AM - Jhett12321: how about if there are two things you pass to the subscription
//4:15 AM - Jhett12321: the first is a list of filters that the event must contain data for all (so if you pass it headshots, and worlds, it MUST be a headshot in one of the given worlds)
//4:16 AM - Jhett12321: the second is a list of filters that must contain at least one match (if you give it a zone and a world, it can be in that zone on any world, or any zone in that specific world)
//4:19 AM - Jhett12321: so the flow would be it would loop through the "must match" list, and check to see if all of the filters contain a match
//4:19 AM - Jhett12321: if those pass, it moves on to the "one match" list, and if any of those filters match it sends the event
public class EventServer
{
    private final EventTracker eventTracker = EventTracker.getInstance();

    private final Config config;

    //API Key Database
    private final String dbUrl;

    //Actions
    private final Map<ActionInfo, Class<? extends Action>> actions = new LinkedHashMap<ActionInfo, Class<? extends Action>>();

    //Client Info
    public final Map<ServerWebSocket, EventServerClient> clientConnections = new ConcurrentHashMap<ServerWebSocket, EventServerClient>();

    public EventServer()
    {
        config = eventTracker.getConfig();
        Vertx vertx = eventTracker.getVertx();

        //API Key Database
        dbUrl = "jdbc:mysql://" + config.getApiDbHost() + "/" + config.getApiDbName();

        //Client Actions
        registerActions();

        vertx.createHttpServer().websocketHandler(new Handler<ServerWebSocket>()
        {
            @Override
            public void handle(final ServerWebSocket clientConnection)
            {
                Map<String, String> queryPairs = new LinkedHashMap<String, String>();

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

                final String apiName = verifyAPIKey(apiKey);
                if (apiName != null)
                {
                    clientConnection.closeHandler(new Handler<Void>()
                    {
                        @Override
                        public void handle(final Void event)
                        {
                            clientConnections.remove(clientConnection);
                            eventTracker.getLogger().info("Client " + apiName + " Disconnected. API Key: " + apiKey);
                        }
                    });

                    clientConnection.exceptionHandler(new Handler<Throwable>()
                    {
                        @Override
                        public void handle(Throwable event)
                        {
                            clientConnections.remove(clientConnection);
                            eventTracker.getLogger().info("Client " + apiName + " Disconnected. API Key: " + apiKey);
                        }
                    });

                    clientConnection.dataHandler(new Handler<Buffer>()
                    {
                        @Override
                        public void handle(Buffer data)
                        {
                            JsonObject message = null;

                            try
                            {
                                message = new JsonObject(data.toString());
                            }
                            catch (Exception e)
                            {
                                clientConnection.writeTextFrame("{\"error\": \"BADJSON\", \"message\": \"You have supplied an invalid JSON string. Please check your syntax.\"}");
                            }

                            if (message != null)
                            {
                                eventTracker.getLogger().info("Client " + apiName + " Sent Valid JSON Message.");
                                eventTracker.getLogger().info(message.encodePrettily());
                                handleClientMessage(clientConnection, message);
                            }

                        }
                    });

                    clientConnections.put(clientConnection, new EventServerClient(clientConnection, apiKey, apiName));
                    eventTracker.getLogger().info("Client " + apiName + " Connected! API Key: " + apiKey);

                    //Send Connection Confirmed Message
                    JsonObject connectMessage = new JsonObject();
                    connectMessage.putString("service", "ps2_events");
                    connectMessage.putString("version", MavenInfo.getVersion());
                    connectMessage.putString("websocket_event", "connectionStateChange");
                    connectMessage.putString("online", "true");

                    clientConnection.writeTextFrame(connectMessage.encodePrettily());

                    //Send Service Status Messages
                    for (Entry<World, WorldInfo> worldEntry : eventTracker.getDynamicDataManager().getAllWorldInfo().entrySet())
                    {
                        JsonObject serviceMessage = new JsonObject();

                        JsonObject payload = new JsonObject();
                        payload.putString("online", worldEntry.getValue().isOnline() ? "1" : "0");
                        payload.putString("world_id", worldEntry.getKey().getID());

                        serviceMessage.putObject("payload", payload);
                        serviceMessage.putString("event_type", "ServiceStateChange");
                    }
                }
                else
                {
                    clientConnection.reject();
                }

            }
        }).listen(config.getServerPort());
    }

    private String verifyAPIKey(String apiKey)
    {
        if (apiKey != null && !apiKey.equals(""))
        {
            Connection dbConnection = null;

            try
            {
                Class.forName("com.mysql.jdbc.Driver");

                dbConnection = DriverManager.getConnection(dbUrl, config.getApiDbUser(), config.getApiDbPassword());

                PreparedStatement query = dbConnection.prepareStatement("SELECT * FROM APIKeys WHERE api_key = ? AND enabled = 1");
                query.setString(1, apiKey);
                ResultSet resultSet = query.executeQuery();

                //Check if API Key exists
                String apiName = null;
                if (resultSet.next())
                {
                    apiName = resultSet.getString("name");
                }

                query.close();
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
        registerAction(ActiveAlerts.class);
        registerAction(ZoneStatus.class);
        registerAction(FacilityStatus.class);
    }

    private void handleClientMessage(ServerWebSocket clientConnection, JsonObject message)
    {
        String eventType = message.getString("event");
        String action = message.getString("action");

        message.removeField("event");
        message.removeField("action");

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
            clientConnection.writeTextFrame("{\"error\": \"unknownAction\", \"message\": \"There is no Action by that name. Please check your syntax, and try again.\"}");
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
            eventTracker.getLogger().warn("Implementing Action Class: " + action.getName() + " is missing a required annotation.");
            return;
        }

        actions.put(info, action);
    }

    public void broadcastEvent(Class<? extends Event> event, JsonObject rawData)
    {
        JsonObject messageToSend = new JsonObject();

        JsonObject eventFilterData = rawData.getObject("filter_data");
        JsonObject eventData = rawData.getObject("event_data");

        messageToSend.putObject("payload", eventData);
        messageToSend.putString("event_type", event.getAnnotation(EventInfo.class).eventName());

        for (Entry<ServerWebSocket, EventServerClient> connection : clientConnections.entrySet())
        {
            Boolean sendMessage = null;
            
            EventType eventType = event.getAnnotation(EventInfo.class).eventType();
            
            if(eventType.equals(EventType.SERVICE))
            {
                sendMessage = true;
            }
            
            else if(eventType.equals(EventType.EVENT))
            {
                //Get Subscription for provided event
                JsonObject subscription = connection.getValue().getSubscription(event);
                
                if (subscription.getString("all").equals("true"))
                {
                    sendMessage = true;
                }

                else
                {
                    for (String subscriptionProperty : subscription.getFieldNames())
                    {
                        if (!subscriptionProperty.equals("all") && !subscriptionProperty.equals("useAND") && !subscriptionProperty.equals("show") && !subscriptionProperty.equals("hide"))
                        {
                            JsonArray filterData = eventFilterData.getArray(subscriptionProperty);

                            if (subscriptionProperty.equals("worlds"))
                            {
                                JsonObject subscriptionValue = subscription.getObject(subscriptionProperty);

                                if (subscriptionValue.size() > 0)
                                {
                                    if (subscriptionValue.containsField((String) filterData.get(0)))
                                    {
                                        JsonArray subscriptionZoneData = subscriptionValue.getObject((String) filterData.get(0)).getArray("zones");
                                        JsonArray zoneData = eventFilterData.getArray("zones");
                                        
                                        if(subscriptionZoneData.size() == 0 || subscriptionZoneData.contains(zoneData.get(0)))
                                        {
                                            sendMessage = true;
                                        }
                                        
                                        else
                                        {
                                            sendMessage = false;
                                        }
                                    }
                                    
                                    else
                                    {
                                        sendMessage = false;
                                    }
                                }
                            }

                            else
                            {
                                JsonArray subscriptionValue = subscription.getArray(subscriptionProperty);

                                if (subscriptionValue.size() > 0)
                                {
                                    if (subscription.getArray("useAND").contains(subscriptionProperty))
                                    {
                                        for (int i = 0; i < filterData.size(); i++)
                                        {
                                            if (subscriptionValue.contains(filterData.get(i)))
                                            {
                                                sendMessage = true;
                                            }
                                            else if (subscriptionValue.size() > 0)
                                            {
                                                sendMessage = false;
                                                break;
                                            }
                                        }
                                    }

                                    else
                                    {
                                        for (int i = 0; i < filterData.size(); i++)
                                        {
                                            if (subscriptionValue.contains(filterData.get(i)))
                                            {
                                                sendMessage = true;
                                            }
                                            else if (subscriptionValue.size() > 0)
                                            {
                                                sendMessage = false;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if (sendMessage != null && !sendMessage)
                            {
                                break;
                            }
                        }
                    }
                }
                
                if (subscription.getArray("show").size() > 0)
                {
                    JsonObject filteredPayload = new JsonObject();

                    for (String field : eventData.getFieldNames())
                    {
                        if (subscription.getArray("show").contains(field))
                        {
                            filteredPayload.putString(field, eventData.getString(field));
                        }
                    }

                    messageToSend.putObject("payload", filteredPayload);
                }

                if (subscription.getArray("hide").size() > 0)
                {
                    JsonObject filteredPayload = messageToSend.getObject("payload");

                    for (String field : eventData.getFieldNames())
                    {
                        if (subscription.getArray("hide").contains(field))
                        {
                            filteredPayload.removeField(field);
                        }
                    }

                    messageToSend.putObject("payload", filteredPayload);
                }
            }
            
            if(sendMessage != null && sendMessage)
            {
                connection.getKey().writeTextFrame(messageToSend.encode());
            }
        }
    }
}
