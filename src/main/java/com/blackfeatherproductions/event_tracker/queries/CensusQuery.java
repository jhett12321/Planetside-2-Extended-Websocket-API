package com.blackfeatherproductions.event_tracker.queries;

import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;

public class CensusQuery
{
    private final String rawQuery;
    private final Environment environment;
    private final boolean allowNoData;
    private final Query callback;
    private final QueryPriority priority;
    private final boolean allowFailure;

    private int failureCount = 0;
    private boolean completed = false;

    public CensusQuery(String rawQuery, QueryPriority priority, Environment environment, boolean allowFailure, boolean allowNoData, Query callback)
    {
        this.rawQuery = rawQuery;
        this.priority = priority;
        this.environment = environment;
        this.allowFailure = allowFailure;
        this.allowNoData = allowNoData;

        this.callback = callback;
    }

    public String getRawQuery()
    {
        return rawQuery;
    }

    public Environment getEnvironment()
    {
        return environment;
    }

    public boolean isNoDataAllowed()
    {
        return allowNoData;
    }

    public Query getCallback()
    {
        completed = true;
        return callback;
    }

    public QueryPriority getPriority()
    {
        return priority;
    }

    public boolean isFailureAllowed()
    {
        return allowFailure;
    }

    public int getFailureCount()
    {
        return failureCount;
    }

    public void incrementFailureCount()
    {
        this.failureCount++;

        if (failureCount > EventTracker.getConfig().getMaxFailures())
        {
            failureCount = EventTracker.getConfig().getMaxFailures();
        }
    }

    public void setFailureCount(int failureCount)
    {
        this.failureCount = failureCount;
    }

    public boolean isCompleted()
    {
        return completed;
    }
}
