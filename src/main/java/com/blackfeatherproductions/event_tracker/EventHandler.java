package com.blackfeatherproductions.event_tracker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.events.AchievementEarnedEvent;
import com.blackfeatherproductions.event_tracker.events.BattleRankEvent;
import com.blackfeatherproductions.event_tracker.events.CombatEvent;
import com.blackfeatherproductions.event_tracker.events.ContinentLockEvent;
import com.blackfeatherproductions.event_tracker.events.DirectiveCompletedEvent;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.FacilityControlEvent;
import com.blackfeatherproductions.event_tracker.events.LoginEvent;
import com.blackfeatherproductions.event_tracker.events.MetagameEvent;
import com.blackfeatherproductions.event_tracker.events.VehicleDestroyEvent;

public class EventHandler
{
    private Map<String, Event> events;
    
    public EventHandler()
    {
        events = new LinkedHashMap<String, Event>();
        
        registerEvent(AchievementEarnedEvent.class);
        registerEvent(BattleRankEvent.class);
        registerEvent(CombatEvent.class);
        registerEvent(ContinentLockEvent.class);
        registerEvent(DirectiveCompletedEvent.class);
        registerEvent(FacilityControlEvent.class);
        registerEvent(LoginEvent.class);
        registerEvent(MetagameEvent.class);
        registerEvent(VehicleDestroyEvent.class);
    }
    
    public boolean handleEvent(String eventName, JsonObject payload)
    {
        Event event = getEvent(eventName);
        if(event == null)
        {
            EventTracker.instance.getLogger().warn("Unhandled Payload! Has Census added a new event?");
            EventTracker.instance.getLogger().warn("Payload data:");
            EventTracker.instance.getLogger().warn(payload.encodePrettily());
            return false; //Event Processor does not exist for this type of event.
        }
        
        event.preProcessEvent(payload);
        return true;
    }
    
    public Event getEvent(String eventName)
    {
        Event event = null;
        
        for(Entry<String, Event> entry : events.entrySet())
        {
            if(eventName.matches(entry.getKey()))
            {
                event = entry.getValue();
            }
        }
        
        return event;
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
            events.put(info.eventNames(), event.newInstance());
        }
        catch(InstantiationException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}
