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
import com.blackfeatherproductions.event_tracker.events.PlanetsideTimeEvent;
import com.blackfeatherproductions.event_tracker.events.PopulationChangeEvent;
import com.blackfeatherproductions.event_tracker.events.VehicleDestroyEvent;

public class EventManager
{
    private Map<String, Event> events;
    
    public EventManager()
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
        registerEvent(PlanetsideTimeEvent.class);
        registerEvent(PopulationChangeEvent.class);
        registerEvent(VehicleDestroyEvent.class);
    }
    
    public boolean handleEvent(String eventName, JsonObject payload)
    {
        boolean eventHandled = false;
        
        for(Entry<String, Event> entry : events.entrySet())
        {
            if(eventName.matches(entry.getKey()))
            {
                entry.getValue().preProcessEvent(payload);
                eventHandled = true;
            }
        }
        
        if(!eventHandled)
        {
            EventTracker.getInstance().getLogger().warn("Unhandled Payload! Has Census added a new event?");
            EventTracker.getInstance().getLogger().warn("Payload data:");
            EventTracker.getInstance().getLogger().warn(payload.encodePrettily());
            return false;
        }
        
        return true;
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
