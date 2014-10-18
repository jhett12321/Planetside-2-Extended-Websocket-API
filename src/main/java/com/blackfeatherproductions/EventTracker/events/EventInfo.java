package com.blackfeatherproductions.EventTracker.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EventInfo
{
    public String eventNames();
}
