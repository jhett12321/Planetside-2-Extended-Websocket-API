package com.blackfeatherproductions.event_tracker.server;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;

public class EventServerClient
{
	//Identification
	private String apiKey;
	private String name;

	//Subscriptions
	private Map<Event, JsonObject> subscriptions = new HashMap<Event, JsonObject>();
	private boolean subscribedToAll = false;
	
	public EventServerClient(String apiKey, String name)
	{
		this.apiKey = apiKey;
		this.name = name;
		
		for(Class<? extends Event> event : EventTracker.getInstance().getEventHandler().getRegisteredEvents())
		{
			EventInfo info = event.getAnnotation(EventInfo.class);
			
			if(!info.filters()[0].equals("no_filtering"))
			{
				JsonObject subscription = new JsonObject();
				
				//Event Specific Filters
				for(String filter : info.filters())
				{
					subscription.putArray(filter, new JsonArray());
				}
				
				//Global Filters
				subscription.putString("all", "false");
				subscription.putObject("worlds", new JsonObject());
				subscription.putArray("useAND", new JsonArray());
				subscription.putArray("show", new JsonArray());
				subscription.putArray("hide", new JsonArray());
			}
		}
	}

	public JsonObject getSubscription(Class<? extends Event> event)
	{
		return subscriptions.get(event);
	}
	
	public JsonObject getSubscriptions()
	{
		//TODO List all subscriptions in same old format.
		//TODO Check if a subscription has been modified.
		return null;
	}

	public boolean isSubscribedToAll()
	{
		return subscribedToAll;
	}

	public void setSubscribedToAll(boolean subscribedToAll)
	{
		this.subscribedToAll = subscribedToAll;
	}
}
