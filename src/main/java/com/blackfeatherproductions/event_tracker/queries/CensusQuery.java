package com.blackfeatherproductions.event_tracker.queries;

import com.blackfeatherproductions.event_tracker.Environment;

public class CensusQuery
{
    private final String rawQuery;
    private final Environment environment;
    private final boolean allowNoData;
    private final Query[] callbacks;
    private final QueryPriority priority;
    private final boolean allowFailure;
    
    public CensusQuery(String rawQuery, QueryPriority priority, Environment environment, boolean allowFailure, boolean allowNoData, Query... callbacks)
    {
        this.rawQuery = rawQuery;
        this.priority = priority;
        this.environment = environment;
        this.allowFailure = allowFailure;
        this.allowNoData = allowNoData;
        this.callbacks = callbacks;
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
    
    public Query[] getCallbacks()
    {
        return callbacks;
    }
    
    public QueryPriority getPriority()
    {
        return priority;
    }
    
    public boolean isFailureAllowed()
    {
        return allowFailure;
    }
}
