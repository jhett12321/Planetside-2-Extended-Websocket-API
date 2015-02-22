package com.blackfeatherproductions.event_tracker.feeds;

import java.util.Date;
import java.util.Map.Entry;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.Config;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.queries.WorldQuery;

public class Census
{
	private HttpClient client;
	private EventTracker eventTracker;
	private Config config;
	private Vertx vertx;
	
	//Connection Stuff
	private WebSocket websocket;
	private boolean websocketConnected = false;
	
	private long lastHeartbeat = 0;
	
    public Census()
    {
        eventTracker = EventTracker.getInstance();
        config = eventTracker.getConfig();
        vertx = eventTracker.getVertx();
        
        client = vertx.createHttpClient();
        
        client.setHost("push.planetside2.com");
        client.setPort(443);
        client.setSSL(true);
        
        connectWebsocket();
        
        //Reconnects the websocket if it is not online, or is not responding.
        vertx.setPeriodic(10000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	if(!websocketConnected)
            	{
            		connectWebsocket();
            	}
            	
            	//If we have not received a heartbeat in the last 5 minutes, restart the connection
            	else if(lastHeartbeat != 0 && (new Date().getTime()) - lastHeartbeat > 300000)
            	{
            		eventTracker.getLogger().error("No hearbeat message received for > 5 minutes. Restarting websocket connection." );
            		websocket.close();
            	}
            }
        });
        
        //Generates event metric messages
        vertx.setPeriodic(1000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	JsonObject payload = new JsonObject();
            	payload.putString("eventsReceived", String.valueOf(eventTracker.getEventsReceived()));
            	payload.putString("eventsProcessed", String.valueOf(eventTracker.getEventsProcessed()));
            	payload.putString("timestamp", String.valueOf(new Date().getTime() / 1000));
            	
            	eventTracker.getEventHandler().handleEvent("EventTrackerMetrics", payload);
            }
        });
    }
    
    public void connectWebsocket()
    {
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
                        JsonObject message = new JsonObject(data.toString());
                        if(message != null)
                        {
                            String serviceType = message.getString("type");
                            
                            if(message.containsField("connected") && message.getString("connected").equals("true"))
                            {
                                eventTracker.getLogger().info("Websocket Secure Connection established to push.planetside.com" );
                                eventTracker.getLogger().info("Subscribing to all events..." );
                                
                                //Send subscription message
                                websocket.writeTextFrame("{\"service\": \"event\",\"action\": \"subscribe\",\"characters\": [\"all\"],\"worlds\": [\"all\"],\"eventNames\": [\"all\"]}");
                                
                                //TODO RecentCharacterIDs Message.
                                //websocket.writeTextFrame("{\"service\": \"event\",\"action\": \"subscribe\",\"characters\": [\"all\"],\"worlds\": [\"all\"],\"eventNames\": [\"all\"]}");
                            }
                            
                            else if(message.containsField("subscription"))
                            {
                            	eventTracker.getLogger().info("Census Confirmed event feed subscription:" );
                            	eventTracker.getLogger().info(message.encodePrettily());
                            }
                            
                            else if(serviceType != null)
                            {
	                            if(serviceType.equals("serviceStateChanged"))
	                            {
	                                if(!websocketConnected)
	                                {
	                                	websocketConnected = true;
	                                }
	                                
	                        		if(message.getString("online").equals("true"))
	                        		{
	                        			updateEndpointStatus(Utils.getWorldIDFromEndpointString(message.getString("detail")), true);
	                        		}
	                        		
	                        		else
	                        		{
	                        			updateEndpointStatus(Utils.getWorldIDFromEndpointString(message.getString("detail")), false);
	                        		}
	                            }
	                            
	                            else if(serviceType.equals("heartbeat"))
	                            {
	                            	JsonObject onlineList = message.getObject("online");
	                            	for(Entry<String, Object> endpoint : onlineList.toMap().entrySet())
	                            	{
	                            		if(endpoint.getValue().equals("true"))
	                            		{
	                            			updateEndpointStatus(Utils.getWorldIDFromEndpointString(endpoint.getKey()), true);
	                            		}
	                            		
	                            		else
	                            		{
	                            			updateEndpointStatus(Utils.getWorldIDFromEndpointString(endpoint.getKey()), false);
	                            		}
	                            	}
	                            }
	                            
	                            else if(serviceType.equals("serviceMessage"))
	                            {
	                                JsonObject payload = message.getObject("payload");
	                                String eventName = payload.getString("event_name");
	                                
	                                EventTracker.getInstance().countReceivedEvent();
	                                
	                                //Check the world status for this event
	                                if(eventTracker.getDynamicDataManager().getWorldInfo(World.getWorldByID(payload.getString("world_id"))).isOnline())
	                                {
	                                    eventTracker.getEventHandler().handleEvent(eventName, payload);
	                                }
	                            }
	                            
	                            else
	                            {
	                            	eventTracker.getLogger().warn("Could not handle message!");
	                            	eventTracker.getLogger().warn(message.encodePrettily());
	                            }
                            }
                            
                            else if(!message.containsField("send this for help"))
                            {
                            	eventTracker.getLogger().warn("Could not handle message!");
                            	eventTracker.getLogger().warn(message.encodePrettily());
                            }
                        }
                    }
                });
                
                websocket.closeHandler(new Handler<Void>()
                {
					@Override
					public void handle(Void arg0)
					{
						onWebsocketDisconnected();
					}
                });
                
                websocket.endHandler(new Handler<Void>()
                {
					@Override
					public void handle(Void arg0)
					{
						onWebsocketDisconnected();
					}
                });
                
                websocket.exceptionHandler(new Handler<Throwable>()
                {
					@Override
					public void handle(Throwable arg0)
					{
						onWebsocketDisconnected();
						arg0.printStackTrace();
					}
                });
            }
        });
    }
    
    private void updateEndpointStatus(String worldID, Boolean newValue)
    {
    	Boolean currentServerStatus = false;
    	World world = World.getWorldByID(worldID);
    	
    	if(eventTracker.getDynamicDataManager().getWorldInfo(World.getWorldByID(worldID)) != null)
    	{
    		currentServerStatus = eventTracker.getDynamicDataManager().getWorldInfo(World.getWorldByID(worldID)).isOnline();
    	}
    	
    	if(currentServerStatus != newValue)
    	{
	    	if(newValue)
	    	{
	    		//Data is (now) being received for this world.
	    		
	    		//Query Census for World Data.
	    		new WorldQuery(worldID);
                
                eventTracker.getLogger().info("Received Census Server State Message. " + world.getName() + " (" + world.getID() + ") is now Online." );
	    	}
	    	
	    	else
	    	{
	    		//No data is being received from this feed. Cached data for this world is invalidated, and must be updated.
	    		eventTracker.getDynamicDataManager().getWorldInfo(world).setOnline(false);
                eventTracker.getLogger().info("Received Census Server State Message. " + world.getName() + " (" + world.getID() + ") is now OFFLINE." );
	    	}
    	}
    }
    
    private void onWebsocketDisconnected()
    {
    	websocketConnected = false;
    	for(World world : World.worlds.values())
    	{
    		eventTracker.getDynamicDataManager().getWorldInfo(world).setOnline(false);
    	}
    }
}
