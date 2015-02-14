package com.blackfeatherproductions.event_tracker.feeds;

import java.util.Date;
import java.util.Map;
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
	
	//Feed Statuses
	private Map<String, Boolean> endpointStatuses;
	
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
            	
            	//If we have not received a heartbeat in the last 2 minutes, restart the connection
            	else if(lastHeartbeat != 0 && (new Date().getTime()) - lastHeartbeat > 120000)
            	{
            		websocket.close();
            	}
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
                    	System.out.println(data.toString());
                    	
                        JsonObject message = new JsonObject(data.toString());
                        if(message != null)
                        {
                            String serviceType = message.getString("type");
                            if(serviceType != null && serviceType == "serviceStateChanged")
                            {
                                eventTracker.getLogger().info("Received Census Server State Message: " + message.encode());
                                
                                if(!websocketConnected)
                                {
                                	websocketConnected = true;
                                }
                            }
                            
                            else if(serviceType == "heartbeat")
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
                            
                            else if(serviceType != null && serviceType == "serviceMessage")
                            {
                                JsonObject payload = message.getObject("payload");
                                String eventName = payload.getString("event_name");
                                
                                eventTracker.getEventHandler().handleEvent(eventName, payload);
                            }
                        }
                    }
                });
                
                websocket.closeHandler(new Handler<Void>()
                {
					@Override
					public void handle(Void arg0)
					{
						websocketConnected = false;
					}
                });
                
                websocket.endHandler(new Handler<Void>()
                {
					@Override
					public void handle(Void arg0)
					{
						websocketConnected = false;
					}
                });
                
                websocket.exceptionHandler(new Handler<Throwable>()
                {
					@Override
					public void handle(Throwable arg0)
					{
						websocketConnected = false;
						arg0.printStackTrace();
					}
                });
                
                //Send subscription message
                websocket.writeTextFrame("{\"service\": \"event\",\"action\": \"subscribe\",\"characters\": [\"all\"],\"worlds\": [\"all\"],\"eventNames\": [\"all\"]}");
            }
        });
    }
    
    private void updateEndpointStatus(String worldID, Boolean newValue)
    {
    	Boolean currentServerStatus = endpointStatuses.get(worldID);
    	
    	if(currentServerStatus != newValue)
    	{
	    	if(newValue == true)
	    	{
	    		new WorldQuery(worldID);
	    	}
	    	
	    	endpointStatuses.put(worldID, newValue);
    	}
    }
}
