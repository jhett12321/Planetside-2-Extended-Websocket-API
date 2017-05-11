package com.blackfeatherproductions.event_tracker.data_static;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Environment
{
    public static List<Environment> environments = new ArrayList<>();
    
    public static Environment PC;
    public static Environment PS4_US;
    public static Environment PS4_EU;
    public static Environment WEBSOCKET_SERVICE;
    
    public final String localName;
    public final String fieldName;
    public final String censusEndpoint;
    public final String websocketEndpoint;
    
    public Environment(String localName, String fieldName, String censusEndpoint, String websocketEndpoint)
    {
        this.localName = localName;
        this.fieldName = fieldName;
        this.censusEndpoint = censusEndpoint;
        this.websocketEndpoint = websocketEndpoint;
    }
    
    public static List<Environment> getEnvironments()
    {
        return environments;
    }
    
    public static Collection<Environment> getValidEnvironments()
    {
        Collection<Environment> validEnvironments = new ArrayList<Environment>();

        for (Environment environment : environments)
        {
            if (environment != Environment.WEBSOCKET_SERVICE)
            {
                validEnvironments.add(environment);
            }
        }

        return validEnvironments;
    }
}
