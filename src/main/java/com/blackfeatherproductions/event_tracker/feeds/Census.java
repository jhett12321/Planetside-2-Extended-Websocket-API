package com.blackfeatherproductions.event_tracker.feeds;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.Config;
import com.blackfeatherproductions.event_tracker.EventTracker;

public class Census
{
	private HttpClient client;
	private EventTracker eventTracker;
	private Config config;
	private Vertx vertx;
	
	//Connection Stuff
	private WebSocket websocket;
	private boolean websocketConnected;
	
	//Event Info
	private long lastEvent;
	
    public Census()
    {
        eventTracker = EventTracker.instance;
        config = eventTracker.getConfig();
        vertx = eventTracker.getVertx();
        
        client = vertx.createHttpClient();
        connectWebsocket();
        
        //Reconnects the websocket if it is not 'online'
        vertx.setPeriodic(10000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	if(!websocketConnected)
            	{
            		connectWebsocket();
            	}
            }
        });
        
        //Checks the service is still sending messages.
        vertx.setPeriodic(10000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	if(websocketConnected)
            	{
            		//websocket.writeTextFrame("{\"service\": \"event\",\"action\": \"echo\",\"payload\": {\"heartbeat\":\"true\"}}");
            		long time = System.currentTimeMillis() / 1000l;
            		if(time - 300 > lastEvent)
            		{
            			websocketConnected = false;
            		}
            	}
            }
        });
    }
    
    public void connectWebsocket()
    {
    	client.connectWebsocket("wss://push.planetside2.com/streaming?service-id=s:" + config.getSoeServiceID(), new Handler<WebSocket>()
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
                        lastEvent = System.currentTimeMillis() / 1000l;
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
}
