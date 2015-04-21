package com.blackfeatherproductions.event_tracker.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EventInfo
{
    public String eventName();

    public String listenedEvents();

    public String[] filters();

    public EventPriority priority();
}
