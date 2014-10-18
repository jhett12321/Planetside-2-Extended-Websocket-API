package com.blackfeatherproductions.EventTracker;

import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;
 
public class EventTracker extends Verticle
{
    public static EventTracker inst;
    
    private Logger logger;
    private Config config;
    private EventHandler eventHandler;
    
    @Override
    public void start()
    {
        inst = this;
        
        logger = container.logger();
        
        config = new Config();
        eventHandler = new EventHandler();
    }
    
    public Logger getLogger()
    {
        return logger;
    }
    
    public Config getConfig()
    {
        return config;
    }
    
    public EventHandler getEventHandler()
    {
        return eventHandler;
    }
}
