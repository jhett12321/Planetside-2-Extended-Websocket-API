package com.blackfeatherproductions.event_tracker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriorityComparator;
import com.blackfeatherproductions.event_tracker.events.PlanetsideTimeEvent;
import com.blackfeatherproductions.event_tracker.events.census.AchievementEarnedEvent;
import com.blackfeatherproductions.event_tracker.events.census.BattleRankEvent;
import com.blackfeatherproductions.event_tracker.events.census.CombatEvent;
import com.blackfeatherproductions.event_tracker.events.census.ContinentLockEvent;
import com.blackfeatherproductions.event_tracker.events.census.DirectiveCompletedEvent;
import com.blackfeatherproductions.event_tracker.events.census.FacilityControlEvent;
import com.blackfeatherproductions.event_tracker.events.census.LoginEvent;
import com.blackfeatherproductions.event_tracker.events.census.MetagameEvent;
import com.blackfeatherproductions.event_tracker.events.census.VehicleDestroyEvent;
import com.blackfeatherproductions.event_tracker.events.extended.PopulationChangeEvent;
import com.blackfeatherproductions.event_tracker.events.listeners.PopulationEventListener;

public class EventManager
{
    private Map<EventInfo, Class<? extends Event>> events = new LinkedHashMap<EventInfo, Class<? extends Event>>();
    private Map<EventInfo, Event> listeners = new LinkedHashMap<EventInfo, Event>();
    private Queue<QueuedEvent> queuedEvents = new PriorityQueue<QueuedEvent>(10, new EventPriorityComparator());
    
    private List<String> unknownEvents = new ArrayList<String>();
    
    public EventManager()
    {
    	//Register events/listeners available for processing.
    	registerListeners();
    	registerCensusEvents();
    	registerExtendedEvents();
        
        //Process Event Queue
        EventTracker.getInstance().getVertx().setPeriodic(100, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	for(int i=0; i<queuedEvents.size(); i++)
            	{
            		queuedEvents.poll().processEvent();
            	}
            }
        });
    }
    
    private void registerCensusEvents()
    {
        registerEvent(AchievementEarnedEvent.class);
        registerEvent(BattleRankEvent.class);
        registerEvent(CombatEvent.class);
        registerEvent(ContinentLockEvent.class);
        registerEvent(DirectiveCompletedEvent.class);
        registerEvent(FacilityControlEvent.class);
        registerEvent(LoginEvent.class);
        registerEvent(MetagameEvent.class);
        registerEvent(PlanetsideTimeEvent.class);
        registerEvent(VehicleDestroyEvent.class);
    }
    
    private void registerExtendedEvents()
    {
        registerEvent(PopulationChangeEvent.class);
    }
    
    private void registerListeners()
    {
    	registerListener(PopulationEventListener.class);
    }
    
    public void handleEvent(String eventName, JsonObject payload)
    {   
        if(Utils.isValidPayload(payload))
        {
            boolean eventHandled = false;
            
            //Listeners
	        for(Entry<EventInfo, Event> entry : listeners.entrySet())
	        {
	            if(eventName.matches(entry.getKey().eventNames()))
	            {
					Event event = entry.getValue();
					
	            	queuedEvents.add(new QueuedEvent(entry.getKey().priority(), event, payload));
	            }
	        }
            
	        //Events
	        for(Entry<EventInfo, Class<? extends Event>> entry : events.entrySet())
	        {
	            if(eventName.matches(entry.getKey().eventNames()))
	            {
					try
					{
						Event event = entry.getValue().newInstance();
						
		            	queuedEvents.add(new QueuedEvent(entry.getKey().priority(), event, payload));
		                eventHandled = true;
					}
					catch (InstantiationException | IllegalAccessException e)
					{
						e.printStackTrace();
					}
	            }
	        }
	        
            if(!eventHandled && !unknownEvents.contains(eventName))
            {
                EventTracker.getInstance().getLogger().warn("[WARNING] Unhandled Payload for event " + eventName + "! Has Census added a new event?");
                EventTracker.getInstance().getLogger().warn("[WARNING] Payload data:");
                EventTracker.getInstance().getLogger().warn(payload.encodePrettily());
                
                unknownEvents.add(eventName);
            }
        }
    }
    
    private void registerEvent(Class<? extends Event> event)
    {
        EventInfo info = event.getAnnotation(EventInfo.class);
        if(info == null)
        {
            return;
        }

        events.put(info, event);
    }
 
    private void registerListener(Class<? extends Event> event)
    {
        EventInfo info = event.getAnnotation(EventInfo.class);
        if(info == null)
        {
            return;
        }

        try
        {
			listeners.put(info, event.newInstance());
		}
        catch (InstantiationException | IllegalAccessException e)
        {
			e.printStackTrace();
		}
    }
}
