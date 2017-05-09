package com.blackfeatherproductions.event_tracker.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EventInfo
{
    EventType eventType();

    String eventName();

    String listenedEvents();

    String[] filters() default
    {
        "no_filtering"
    };

    EventPriority priority();
}
