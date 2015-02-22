package com.blackfeatherproductions.event_tracker.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;

public class EventServerClient
{
	//Connection
	private ServerWebSocket clientConnection;
	
	//Identification
	private String apiKey;
	private String name;

	//Subscriptions
	private Map<Class<? extends Event>, JsonObject> subscriptions = new HashMap<Class<? extends Event>, JsonObject>();
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
		
		for(Class<? extends Event> eventType : subscriptions.keySet())
		{
			if(eventType.getAnnotation(EventInfo.class).eventName().equals(eventName))
			{
				event = eventType;
			}
		}
		
		if(event != null)
		{
			JsonObject subscription = subscriptions.get(event);
			
			if(action.equals("subscribe"))
			{
				for(String property : messageFilters.toMap().keySet())
				{
					if(subscription.containsField(property))
					{
						if(property.equals("all"))
						{
							if(messageFilters.getString(property).equals("true"))
							{
								subscription.putString(property, "true");
							}
						}
						
						else if(!property.equals("worlds") && !property.equals("zones") || (!messageFilters.containsField("worlds") && property.equals("zones")))
						{
							for(int i=0; i<messageFilters.getArray(property).size(); i++)
							{
								if(!subscription.getArray(property).contains(messageFilters.getArray(property).get(i)))
								{
									subscription.getArray(property).add(messageFilters.getArray(property).get(i));
								}
							}
						}
						
						else if(property.equals("worlds"))
						{
							for(int i=0; i<messageFilters.getArray(property).size(); i++)
							{
								if(!subscription.getObject(property).containsField((String) messageFilters.getArray(property).get(i)))
								{
									JsonObject worldObject = new JsonObject();
									worldObject.putArray("zones", new JsonArray());
									
									subscription.getObject(property).putObject((String) messageFilters.getArray(property).get(i), worldObject);
								}
								
								if(messageFilters.containsField("zones"))
								{
									for(int j=0; j<messageFilters.getArray("zones").size(); j++)
									{
										if(!subscription.getObject(property).getObject((String) messageFilters.getArray(property).get(i)).getArray("zones").contains(messageFilters.getArray("zones").get(j)))
										{
											subscription.getObject(property).getObject((String) messageFilters.getArray(property).get(i)).getArray("zones").add(messageFilters.getArray("zones").get(j));
										}
									}
								}
							}
						}
					}
				}
			}
		
			else if(action.equals("unsubscribe"))
			{
				for(String property : messageFilters.toMap().keySet())
				{
					if(subscription.containsField(property))
					{
						if(property.equals("all"))
						{
							if(messageFilters.getString(property).equals("false"))
							{
								subscription.putString(property, "false");
							}
						}
						
						else if(!property.equals("world") && !property.equals("zones") || (!messageFilters.containsField("worlds") && property.equals("zones")))
						{
							JsonArray filteredArray = new JsonArray();
							
							for(int i=0; i<subscription.getArray(property).size(); i++)
							{
								if(!messageFilters.getArray(property).contains(subscription.getArray(property).get(i)))
								{
									filteredArray.add(messageFilters.getArray(property).get(i));
								}
							}
							
							subscription.putArray(property, filteredArray);
						}
						
						else if(property.equals("worlds"))
						{
							JsonObject filteredObject = new JsonObject();
							
							for(Entry<String, Object> world : subscription.getObject(property).toMap().entrySet())
							{
								if(!messageFilters.getObject(property).containsField(world.getKey()))
								{
									JsonObject worldObject = new JsonObject();
									worldObject.putArray("zones", new JsonArray());
									
									filteredObject.putObject(world.getKey(), worldObject);
								}
								
								if(subscription.containsField("zones"))
								{
									for(int i=0; i<subscription.getArray("zones").size(); i++)
									{
										if(!messageFilters.getObject(property).getObject(world.getKey()).getArray("zones").contains(messageFilters.getArray("zones").get(i)))
										{
											filteredObject.getObject(world.getKey()).getArray("zones").add(messageFilters.getArray("zones").get(i));
										}
									}
								}
							}
							
							subscription.putObject(property, filteredObject);
						}
					}
				}

			}
			
			else if(action.equals("unsubcribeAll"))
			{
				subscription = new JsonObject();
				
				subscription.putString("all", "false");
				subscription.putObject("worlds", new JsonObject());
				subscription.putArray("useAND", new JsonArray());
				subscription.putArray("show", new JsonArray());
				subscription.putArray("hide", new JsonArray());
				
				subscriptions.put(event, subscription);
			}
		}
		
		else if(eventName == null && action.equals("unsubscribeAll"))
		{
			clearSubscriptions();
		}
			
		else if(eventName != null)
		{
			clientConnection.writeTextFrame("{\"error\": \"unknownEvent\", \"message\": \"There is no Event Type by that name. Please check your syntax, and try again.\"}");
		}
		
		JsonObject returnSubscriptions = new JsonObject();
		
		JsonObject returnObject = new JsonObject();
		
		for(Entry<Class<? extends Event>, JsonObject> subscription : subscriptions.entrySet())
		{
			for(Entry<String, Object> element : subscription.getValue().toMap().entrySet())
			{
				String property = element.getKey();
				
				if((!property.equals("all") && !property.equals("worlds") && subscription.getValue().getArray(property).size() > 0)
				|| (property.equals("all") && subscription.getValue().getString(property).equals("true"))
				|| (property.equals("worlds") && subscription.getValue().getObject(property).size() > 0))
				{
					returnObject.putObject(subscription.getKey().getAnnotation(EventInfo.class).eventName(), subscription.getValue());
					break;
				}
			}
		}
		
		returnSubscriptions.putObject("subscriptions", returnObject);
		returnSubscriptions.putString("action", action);
		
		clientConnection.writeTextFrame(returnObject.encode());
	}
	
	private void clearSubscriptions()
	{
		this.subscriptions.clear();
		
		for(Class<? extends Event> event : EventTracker.getInstance().getEventHandler().getRegisteredEvents())
		{
			EventInfo info = event.getAnnotation(EventInfo.class);
			
			JsonObject subscription = new JsonObject();
			if(!info.filters()[0].equals("no_filtering"))
			{
				//Event Specific Filters
				for(String filter : info.filters())
				{
					subscription.putArray(filter, new JsonArray());
				}
				
				//Global Filters
				subscription.putObject("worlds", new JsonObject());
				subscription.putArray("useAND", new JsonArray());
			}

			subscription.putString("all", "false");
			subscription.putArray("show", new JsonArray());
			subscription.putArray("hide", new JsonArray());
			
			//Add blank subscription to this client;
			subscriptions.put(event, subscription);
		}
	}
}
