package com.blackfeatherproductions.event_tracker.feeds;

import java.util.Date;
import java.util.Map.Entry;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.Config;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.queries.WorldQuery;

public class Census
{
    private final EventTracker eventTracker = EventTracker.getInstance();
    private final Config config = eventTracker.getConfig();

    private final HttpClient client;
    private WebSocket websocket;
    
    //Connection Stuff
    private WebsocketConnectState websocketConnectState = WebsocketConnectState.CLOSED;
    private long lastHeartbeat = 0;
    private long startTime = 0;

    //================================================================================
    // Constructor
    //================================================================================
    
    public Census()
    {
        Vertx vertx = eventTracker.getVertx();

        client = vertx.createHttpClient();

        //Websocket & Client Attributes
        client.setHost("push.planetside2.com");
        client.setPort(443);
        client.setSSL(true);
        client.setMaxWebSocketFrameSize(1000000);
        client.setReceiveBufferSize(1000000);
        client.setSendBufferSize(1000000);
        
        //Disconnects the websocket if we receive an exception.
        client.exceptionHandler(new Handler<Throwable>()
        {
            @Override
            public void handle(Throwable arg0)
            {
                eventTracker.getLogger().error("Websocket connection lost: A fatal connection exception occured. (See below for stack trace)");
                disconnectWebsocket();
                arg0.printStackTrace();
            }
        });

        //Reconnects the websocket if it is not online, or is not responding.
        vertx.setPeriodic(10000, new Handler<Long>()
        {
            @Override
            public void handle(Long timerID)
            {
                //Connect the websocket if it is closed.
                if (websocketConnectState.equals(WebsocketConnectState.CLOSED))
                {
                    eventTracker.getLogger().info("Reconnecting...");
                    connectWebsocket();
                }

                //If we have not received a heartbeat in the last 2 minutes, disconnect, then restart the connection
                else if (!websocketConnectState.equals(WebsocketConnectState.CONNECTING) && lastHeartbeat != 0 && (new Date().getTime()) - lastHeartbeat > 120000)
                {
                    eventTracker.getLogger().error("No hearbeat message received for > 5 minutes. Restarting websocket connection.");
                    disconnectWebsocket();
                }
                
                //If the current connection attempt has lasted longer than a minute, cancel the attempt and try again.
                else if(websocketConnectState.equals(WebsocketConnectState.CONNECTING) && startTime != 0 && (new Date().getTime()) - startTime > 60000)
                {
                    eventTracker.getLogger().error("Websocket Connection Timeout Reached. Retrying connection...");
                    disconnectWebsocket();
                }
            }
        });

        connectWebsocket();
    }
    
    //================================================================================
    // Websocket Connection Management
    //================================================================================
    
    public void connectWebsocket()
    {
        websocketConnectState = WebsocketConnectState.CONNECTING;
        startTime = new Date().getTime();
        
        client.connectWebsocket("/streaming?service-id=s:" + config.getSoeServiceID(), new Handler<WebSocket>()
        {
            @Override
            public void handle(WebSocket ws)
            {
                websocket = ws;
                websocket.dataHandler(new Handler<Buffer>()
                {
                    @Override
                    public void handle(Buffer data)
                    {
                        //We received a valid message from census.
                        JsonObject message = new JsonObject(data.toString());
                        String serviceType = message.getString("type");

                        if (message.containsField("connected") && message.getString("connected").equals("true"))
                        {
                            //We are now connected.
                            //Set our connection state to open.
                            websocketConnectState = WebsocketConnectState.OPEN;
                            eventTracker.getLogger().info("Websocket Secure Connection established to push.planetside.com");

                            //Get recent character ID's for population.
                            eventTracker.getLogger().info("Requesting seen Character IDs...");
                            websocket.writeTextFrame("{\"service\":\"event\", \"action\":\"recentCharacterIds\"}");
                        }

                        else if (message.containsField("subscription"))
                        {
                            eventTracker.getLogger().info("Census Confirmed event feed subscription:");
                            eventTracker.getLogger().info(message.encodePrettily());
                        }

                        else if (serviceType != null)
                        {
                            switch (serviceType)
                            {
                                case "serviceStateChanged":
                                {
                                    if (message.getString("online").equals("true"))
                                    {
                                        updateEndpointStatus(Utils.getWorldIDFromEndpointString(message.getString("detail")), true);
                                    }

                                    else
                                    {
                                        updateEndpointStatus(Utils.getWorldIDFromEndpointString(message.getString("detail")), false);
                                    }
                                    
                                    break;
                                }
                                case "heartbeat":
                                {
                                    lastHeartbeat = new Date().getTime();
                                    JsonObject onlineList = message.getObject("online");
                                    
                                    for (Entry<String, Object> endpoint : onlineList.toMap().entrySet())
                                    {
                                        if (endpoint.getValue().equals("true"))
                                        {
                                            updateEndpointStatus(Utils.getWorldIDFromEndpointString(endpoint.getKey()), true);
                                        }

                                        else
                                        {
                                            updateEndpointStatus(Utils.getWorldIDFromEndpointString(endpoint.getKey()), false);
                                        }
                                    }
                                    
                                    break;
                                }
                                case "serviceMessage":
                                {
                                    JsonObject payload = message.getObject("payload");
                                    String eventName = payload.getString("event_name");
                                    
                                    processServiceMessage(payload, eventName);
                                    break;
                                }
                                default:
                                {
                                    eventTracker.getLogger().warn("Could not handle message!");
                                    eventTracker.getLogger().warn(message.encodePrettily());
                                    break;
                                }
                            }
                        }

                        else if (!message.containsField("send this for help"))
                        {
                            eventTracker.getLogger().warn("Could not handle message!");
                            eventTracker.getLogger().warn(message.encodePrettily());
                        }
                    }
                });

                websocket.closeHandler(new Handler<Void>()
                {
                    @Override
                    public void handle(Void arg0)
                    {
                        eventTracker.getLogger().error("Websocket connection lost: The websocket connection was closed.");
                        disconnectWebsocket();
                    }
                });

                websocket.endHandler(new Handler<Void>()
                {
                    @Override
                    public void handle(Void arg0)
                    {
                        eventTracker.getLogger().error("Websocket connection lost: The websocket connection ended.");
                        disconnectWebsocket();
                    }
                });
            }
        });
    }
    
    private void disconnectWebsocket()
    {
        //Close the websocket if not already, and set it to null to free unused memory.
        if(websocket != null)
        {
            try
            {
                websocket.close();
            }
            catch(IllegalStateException e)
            {
                
            }
            websocket = null;
        }
        
        //Set the connection state to closed
        websocketConnectState = WebsocketConnectState.CLOSED;

        //Update endpoints if they are not already offline.
        for (WorldInfo world : eventTracker.getDynamicDataManager().getAllWorldInfo().values())
        {
            world.setOnline(false);
        }
    }
    
    //================================================================================
    // Message Management
    //================================================================================

    private void processServiceMessage(JsonObject payload, String eventName)
    {
        //This is the final init step. Process the character list.
        if (payload.containsField("recent_character_id_list"))
        {
            eventTracker.getLogger().info("Character List Received!");
            eventTracker.getLogger().info("Subscribing to all events...");

            //Send subscription message
            websocket.writeTextFrame("{\"service\": \"event\",\"action\": \"subscribe\",\"characters\": [\"all\"],\"worlds\": [\"all\"],\"eventNames\": [\"all\"]}");

            JsonArray recentCharacterIDList = payload.getArray("recent_character_id_list");
            for (int i = 0; i < recentCharacterIDList.size(); i++)
            {
                JsonObject characterPayload = new JsonObject();
                characterPayload.putString("character_id", (String) recentCharacterIDList.get(i));
                characterPayload.putString("event_name", "CharacterList");

                eventTracker.getEventHandler().handleEvent("CharacterList", characterPayload);
            }
        }

        //This is a regular event.
        //Don't send this event if the world is not online, otherwise send it to the event manage for processing.
        else if (eventTracker.getDynamicDataManager().getWorldInfo(World.getWorldByID(payload.getString("world_id"))).isOnline())
        {
            eventTracker.getEventHandler().handleEvent(eventName, payload);
        }
    }
    
    private void updateEndpointStatus(String worldID, Boolean newValue)
    {
        Boolean currentServerStatus = false;
        World world = World.getWorldByID(worldID);

        if (eventTracker.getDynamicDataManager().getWorldInfo(World.getWorldByID(worldID)) != null)
        {
            currentServerStatus = eventTracker.getDynamicDataManager().getWorldInfo(World.getWorldByID(worldID)).isOnline();
        }

        if (!currentServerStatus.equals(newValue))
        {
            if (newValue)
            {
	    	//Data is (now) being received for this world.
                //Query Census for World Data.
                new WorldQuery(worldID);
            }

            else
            {
                //No data is being received from this feed. Cached data for this world is invalidated, and must be updated.
                eventTracker.getDynamicDataManager().getWorldInfo(world).setOnline(false);
            }
        }
    }
}
