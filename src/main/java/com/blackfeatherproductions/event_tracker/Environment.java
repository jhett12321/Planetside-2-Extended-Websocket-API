package com.blackfeatherproductions.event_tracker;

import java.util.ArrayList;
import java.util.List;

public class Environment
{
    public static final Environment PC = new Environment("PC", "ps2:v2");
    public static final Environment PS4_US = new Environment("PS4_US", "ps2ps4us:v2");
    public static final Environment PS4_EU = new Environment("PS4_EU", "ps2ps4eu:v2");
    public static final Environment WEBSOCKET_SERVICE = new Environment("WEBSOCKET_SERVICE", null);
    
    public final String localName;
    public final String censusEndpoint;
    
    private static List<Environment> environments = new ArrayList<>();
    
    public Environment(String localName, String censusEndpoint)
    {
        this.localName = localName;
        this.censusEndpoint = censusEndpoint;
        
        //TODO Unsafe?
        environments.add(this);
    }
    
    public static List<Environment> getEnvironments()
    {
        return environments;
    }
}
