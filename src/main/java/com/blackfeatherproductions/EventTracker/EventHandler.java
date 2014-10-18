package com.blackfeatherproductions.EventTracker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.EventTracker.events.AchievementEarnedEvent;
import com.blackfeatherproductions.EventTracker.events.BattleRankEvent;
import com.blackfeatherproductions.EventTracker.events.CombatEvent;
import com.blackfeatherproductions.EventTracker.events.ContinentLockEvent;
import com.blackfeatherproductions.EventTracker.events.DirectiveCompletedEvent;
import com.blackfeatherproductions.EventTracker.events.Event;
import com.blackfeatherproductions.EventTracker.events.EventInfo;
import com.blackfeatherproductions.EventTracker.events.FacilityControlEvent;
import com.blackfeatherproductions.EventTracker.events.LoginEvent;
import com.blackfeatherproductions.EventTracker.events.MetagameEvent;
import com.blackfeatherproductions.EventTracker.events.VehicleDestroyEvent;

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
            return false; //Event Processor does not exist for this type of event.
        }
        
        event.processEvent(payload);
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
