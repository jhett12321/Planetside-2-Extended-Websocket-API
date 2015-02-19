package com.blackfeatherproductions.event_tracker.server.actions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ActionInfo
{
    public String actionNames();
}

