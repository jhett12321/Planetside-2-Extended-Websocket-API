package com.blackfeatherproductions.event_tracker.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.Config;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.server.actions.Action;
import com.blackfeatherproductions.event_tracker.server.actions.ActionInfo;
import com.blackfeatherproductions.event_tracker.server.actions.ActiveAlerts;
import com.blackfeatherproductions.event_tracker.server.actions.FacilityStatus;
import com.blackfeatherproductions.event_tracker.server.actions.ZoneStatus;

public class EventServer
{
	private Map<ActionInfo, Class<? extends Action>> actions = new LinkedHashMap<ActionInfo, Class<? extends Action>>();
	
    public Map<ServerWebSocket, EventServerClient> clientConnections = new ConcurrentHashMap<ServerWebSocket,EventServerClient>();
    
    public EventServer()
    {
        final EventTracker eventTracker = EventTracker.getInstance();
        Config config = eventTracker.getConfig();
        Vertx vertx = eventTracker.getVertx();
        
        registerActions();
        
        vertx.createHttpServer().websocketHandler(new Handler<ServerWebSocket>()
        {
            public void handle(final ServerWebSocket clientConnection)
            {
            	String apiKey = clientConnection.query();
            	eventTracker.getLogger().info(apiKey);

            	//TODO Verify API Key
            	if (true)
            	{
	                clientConnection.closeHandler(new Handler<Void>()
	                {
						@Override
						public void handle(final Void event)
						{
							clientConnections.remove(clientConnection);
						}
	                });
	                
	                clientConnection.exceptionHandler(new Handler<Throwable>()
	                {
						@Override
						public void handle(Throwable event)
						{
							clientConnections.remove(clientConnection);
						}
	                });
	                
	                clientConnection.dataHandler(new Handler<Buffer>()
	                {
	                    public void handle(Buffer data)
	                    {
	                    	JsonObject message = null;
	                    	
	                    	try
	                    	{
	                    		message = new JsonObject(data.toString());
	                    	}
	                    	catch(Exception e)
	                    	{
	                    		clientConnection.writeTextFrame("{\"error\": \"BADJSON\", \"message\": \"You have supplied an invalid JSON string. Please check your syntax.\"}");
	                    	}
	                    	
	                    	if(message != null)
	                    	{
	                    		//TODO
	                    		//handleClientMessage(clientConnection, message);
	                    	}
	
	                    }
	                });
	                 
	                //TODO Initialize client subscriptions, send connection success message.
	                eventTracker.getLogger().info("Creating Connection.");
	                clientConnections.put(clientConnection, new EventServerClient("example", "Example User"));
            	}
            	else
            	{
            		clientConnection.reject();
            	}
                
            }
        }).listen(config.getServerPort());
    }
    
    private void registerActions()
    {
    	registerAction(ActiveAlerts.class);
    	registerAction(ZoneStatus.class);
    	registerAction(FacilityStatus.class);
    }
    
    public void BroadcastEvent(Class<? extends Event> event, JsonObject rawData)
    {
        JsonObject messageToSend = new JsonObject();
        
        messageToSend.putObject("payload", rawData.getObject("event_data"));
        messageToSend.putString("event_type", rawData.getString("event_type"));
        
        for(Entry<ServerWebSocket, EventServerClient> connection : clientConnections.entrySet())
        {
            //TODO Filter events
        	connection.getValue();
        	if((rawData.getString("event_type").equals("EventTrackerMetricsEvent")))
        	{
        		connection.getKey().writeTextFrame(messageToSend.encode());
        	}
        }
    }
    
    private void registerAction(Class<? extends Action> action)
    {
        ActionInfo info = action.getAnnotation(ActionInfo.class);
        if(info == null)
        {
        	EventTracker.getInstance().getLogger().warn("Implementing Action Class: " + action.getName() + " is missing a required annotation.");
            return;
        }

       	actions.put(info, action);
    }
    
    //TODO
    /*
    private void handleClientMessage(ServerWebSocket clientConnection, JsonObject message)
    {
		String eventType = message.getString("event");
		String action = message.getString("action");
		
		if(!action.matches("/subscribe|unsubscribe|unsubscribeall/i"))
		{
			handleAction(clientConnection, action, message);
		}
		
		else
		{
			
		}
    }
    
    private void handleAction(ServerWebSocket clientConnection, String actionName, JsonObject actionData)
    {
        for(Entry<ActionInfo, Class<? extends Action>> entry : actions.entrySet())
        {
            if(actionName.matches(entry.getKey().actionNames()))
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
    }*/
}
