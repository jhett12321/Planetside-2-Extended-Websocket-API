package com.blackfeatherproductions.event_tracker;

import java.util.ArrayList;
import java.util.Collection;
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
import com.blackfeatherproductions.event_tracker.events.census.AchievementEarnedEvent;
import com.blackfeatherproductions.event_tracker.events.census.BattleRankEvent;
import com.blackfeatherproductions.event_tracker.events.census.CombatEvent;
import com.blackfeatherproductions.event_tracker.events.census.ContinentLockEvent;
import com.blackfeatherproductions.event_tracker.events.census.ContinentUnlockEvent;
import com.blackfeatherproductions.event_tracker.events.census.DirectiveCompletedEvent;
import com.blackfeatherproductions.event_tracker.events.census.ExperienceEarnedEvent;
import com.blackfeatherproductions.event_tracker.events.census.FacilityControlEvent;
import com.blackfeatherproductions.event_tracker.events.census.ItemAddedEvent;
import com.blackfeatherproductions.event_tracker.events.census.LoginEvent;
import com.blackfeatherproductions.event_tracker.events.census.MetagameEvent;
import com.blackfeatherproductions.event_tracker.events.census.PlayerFacilityControlEvent;
import com.blackfeatherproductions.event_tracker.events.census.SkillAddedEvent;
import com.blackfeatherproductions.event_tracker.events.census.VehicleDestroyEvent;
import com.blackfeatherproductions.event_tracker.events.extended.PlanetsideTimeEvent;
import com.blackfeatherproductions.event_tracker.events.extended.PopulationChangeEvent;
import com.blackfeatherproductions.event_tracker.events.service.ServiceStateChangeEvent;
import com.blackfeatherproductions.event_tracker.events.listeners.PopulationEventListener;

public class EventManager
{
    private final EventTracker eventTracker = EventTracker.getInstance();

    private final Map<EventInfo, Class<? extends Event>> events = new LinkedHashMap<EventInfo, Class<? extends Event>>();
    private final Map<EventInfo, Class<? extends Event>> listeners = new LinkedHashMap<EventInfo, Class<? extends Event>>();

    private final Queue<QueuedEvent> queuedEvents = new PriorityQueue<QueuedEvent>(10, new EventPriorityComparator());
    private final Queue<QueuedEvent> queuedListeners = new PriorityQueue<QueuedEvent>(10, new EventPriorityComparator());

    private final List<String> unknownEvents = new ArrayList<String>();

    public EventManager()
    {
        //Register events/listeners available for processing.
        registerEvents();
        
        //Process Event Queue
        eventTracker.getVertx().setPeriodic(100, new Handler<Long>()
        {
            @Override
            public void handle(Long timerID)
            {
                for (int i = 0; i < queuedListeners.size(); i++)
                {
                    queuedListeners.poll().processEvent();
                }

                for (int i = 0; i < queuedEvents.size(); i++)
                {
                    queuedEvents.poll().processEvent();
                }
            }
        });
    }

    public void handleEvent(String eventName, JsonObject payload, Environment environment)
    {
        if (Utils.isValidPayload(payload))
        {
            boolean eventHandled = false;

            //Listeners
            for (Entry<EventInfo, Class<? extends Event>> entry : listeners.entrySet())
            {
                if (eventName.matches(entry.getKey().listenedEvents()))
                {
                    try
                    {
                        Event listener = entry.getValue().newInstance();

                        queuedListeners.add(new QueuedEvent(entry.getKey().priority(), listener, payload, environment));
                        eventHandled = true;
                    }
                    catch (InstantiationException | IllegalAccessException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            //Events
            for (Entry<EventInfo, Class<? extends Event>> entry : events.entrySet())
            {
                if (eventName.matches(entry.getKey().listenedEvents()))
                {
                    try
                    {
                        Event event = entry.getValue().newInstance();

                        queuedEvents.add(new QueuedEvent(entry.getKey().priority(), event, payload, environment));
                        eventHandled = true;
                    }
                    catch (InstantiationException | IllegalAccessException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            if (!eventHandled && !unknownEvents.contains(eventName))
            {
                eventTracker.getLogger().warn("Unhandled Payload for event " + eventName + "! Has Census added a new event?");
                eventTracker.getLogger().warn("Payload data:");
                eventTracker.getLogger().warn(payload.encodePrettily());

                unknownEvents.add(eventName);
            }
        }
    }
    
    private void registerEvents()
    {
        //Listeners
        //registerEvent(PopulationEventListener.class);
        
        //Service Events
        registerEvent(ServiceStateChangeEvent.class);
        
        //Census Events
        registerEvent(AchievementEarnedEvent.class);
        registerEvent(BattleRankEvent.class);
        registerEvent(CombatEvent.class);
        registerEvent(ContinentLockEvent.class);
        registerEvent(ContinentUnlockEvent.class);
        registerEvent(DirectiveCompletedEvent.class);
        registerEvent(ExperienceEarnedEvent.class);
        registerEvent(FacilityControlEvent.class);
        registerEvent(ItemAddedEvent.class);
        registerEvent(LoginEvent.class);
        registerEvent(MetagameEvent.class);
        registerEvent(PlayerFacilityControlEvent.class);
        registerEvent(SkillAddedEvent.class);
        registerEvent(VehicleDestroyEvent.class);
        
        //Extended Events
        registerEvent(PopulationChangeEvent.class);
        registerEvent(PlanetsideTimeEvent.class);
    }

    /**
     * This method is used to register an event to be handled by the
     * EventManager. <br/>
     * Event payloads are sent to these event classes based on the event names
     * listed in the class' annotation. <br/>
     *
     * @param event A class fully implementing the event interface.
     */
    public void registerEvent(Class<? extends Event> event)
    {
        EventInfo info = event.getAnnotation(EventInfo.class);
        if (info == null)
        {
            eventTracker.getLogger().warn("Implementing Event Class: " + event.getName() + " is missing a required annotation.");
            return;
        }
        
        switch(info.eventType())
        {
            case SERVICE:
            case EVENT:
                events.put(info, event);
                break;
            case LISTENER:
                listeners.put(info, event);
                break;
        }
    }

    public Collection<Class<? extends Event>> getRegisteredEvents()
    {
        return events.values();
    }
}
