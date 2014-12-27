package com.blackfeatherproductions.event_tracker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.events.AchievementEarnedEvent;
import com.blackfeatherproductions.event_tracker.events.BattleRankEvent;
import com.blackfeatherproductions.event_tracker.events.CombatEvent;
import com.blackfeatherproductions.event_tracker.events.ContinentLockEvent;
import com.blackfeatherproductions.event_tracker.events.DirectiveCompletedEvent;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriorityComparator;
import com.blackfeatherproductions.event_tracker.events.FacilityControlEvent;
import com.blackfeatherproductions.event_tracker.events.LoginEvent;
import com.blackfeatherproductions.event_tracker.events.MetagameEvent;
import com.blackfeatherproductions.event_tracker.events.PlanetsideTimeEvent;
import com.blackfeatherproductions.event_tracker.events.VehicleDestroyEvent;

public class EventManager
{
    private Map<EventInfo, Event> events = new LinkedHashMap<EventInfo, Event>();
    private Queue<QueuedEvent> queuedEvents = new PriorityQueue<QueuedEvent>(10, new EventPriorityComparator());
    
    public EventManager()
    {
    	//Register events available for processing.
        registerEvent(AchievementEarnedEvent.class);
        registerEvent(BattleRankEvent.class);
        registerEvent(CombatEvent.class);
        registerEvent(ContinentLockEvent.class);
        registerEvent(DirectiveCompletedEvent.class);
        registerEvent(FacilityControlEvent.class);
        registerEvent(LoginEvent.class);
        registerEvent(MetagameEvent.class);
        registerEvent(PlanetsideTimeEvent.class);
        //registerEvent(PopulationChangeEvent.class); //TODO 1.1 Population Event
        registerEvent(VehicleDestroyEvent.class);
        
        //Process Event Queue
        EventTracker.getInstance().getVertx().setPeriodic(100, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	for(QueuedEvent event : queuedEvents)
            	{
        			event.processEvent();
        			queuedEvents.remove(event);
            	}
            }
        });
    }
    
    public void handleEvent(String eventName, JsonObject payload)
    {   
        if(Utils.isValidPayload(payload))
        {
            boolean eventHandled = false;
            
	        for(Entry<EventInfo, Event> entry : events.entrySet())
	        {
	            if(eventName.matches(entry.getKey().eventNames()))
	            {
	            	queuedEvents.add(new QueuedEvent(entry.getKey().priority(), entry.getValue(), payload));
	                eventHandled = true;
	            }
	            
	            if(!eventHandled)
	            {
	                EventTracker.getInstance().getLogger().warn("Unhandled Payload! Has Census added a new event?");
	                EventTracker.getInstance().getLogger().warn("Payload data:");
	                EventTracker.getInstance().getLogger().warn(payload.encodePrettily());
	            }
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
        try
        {
            events.put(info, event.newInstance());
        }
        catch(InstantiationException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}
