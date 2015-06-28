package com.blackfeatherproductions.event_tracker.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EventInfo
{
    public EventType eventType();

    public String eventName();

    public String listenedEvents();

    public String[] filters() default 
    {
        "no_filtering"
    };

    public EventPriority priority();
}
