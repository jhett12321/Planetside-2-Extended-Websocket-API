package com.blackfeatherproductions.event_tracker;

import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.blackfeatherproductions.event_tracker.feeds.Census;
import com.blackfeatherproductions.event_tracker.feeds.CensusRest;
import com.blackfeatherproductions.event_tracker.server.EventServer;
 
public class EventTracker extends Verticle
{
    //Singleton
    private static EventTracker instance;
    
    //Vertx
    private Logger logger;
    
    //Data
    private Config config;
    
    //Managers
    private EventManager eventManager;
	private DynamicDataManager dynamicDataManager;
    private QueryManager queryManager;
    private Utils utils;

    //Event Server
	private EventServer eventServer;
	
	//Metrics
	private int eventsReceived = 0;
	private int eventsProcessed = 0;
    
    @Override
    public void start()
    {
        instance = this; //Singleton
        
        logger = container.logger();
        
        //Config
        config = new Config();
        
        //Static Data
        new StaticDataManager();
        
        //Managers and Handlers
        dynamicDataManager = new DynamicDataManager();
        queryManager = new QueryManager();
        utils = new Utils();
        eventManager = new EventManager();
        
        //Event Server
        eventServer = new EventServer();
        
        //Feeds
        new Census();
        new CensusRest();
    }
    
    public static EventTracker getInstance()
    {
		return instance;
	}

	public QueryManager getQueryManager()
    {
		return queryManager;
	}

	public Utils getUtils()
    {
        return utils;
    }
    
    public DynamicDataManager getDynamicDataManager()
    {
    	return dynamicDataManager;
    }

    public EventServer getEventServer()
    {
		return eventServer;
	}

	public Logger getLogger()
    {
        return logger;
    }
    
    public Config getConfig()
    {
        return config;
    }
    
    public EventManager getEventHandler()
    {
        return eventManager;
    }

	public int getEventsReceived()
	{
		int eventsReceived = this.eventsReceived;
		
		this.eventsReceived = 0;
		return eventsReceived;
	}

	public int getEventsProcessed()
	{
		int eventsProcessed = this.eventsProcessed;
		
		this.eventsProcessed = 0;
		return eventsProcessed;
	}

	public void countReceivedEvent()
	{
		this.eventsReceived++;
	}
	
	public void countProcessedEvent()
	{
		this.eventsProcessed++;
	}
}
