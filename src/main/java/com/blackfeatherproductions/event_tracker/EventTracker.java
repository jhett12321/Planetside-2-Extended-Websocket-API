package com.blackfeatherproductions.event_tracker;

import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.blackfeatherproductions.event_tracker.events.extended.population.PopulationManager;
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
    private PopulationManager populationManager;
    private Utils utils;

    //Event Server
	private EventServer eventServer;
	
	//Metrics
	private int eventsReceived = 0;
	private int eventsProcessed = 0;
    
    @Override
    public void start()
    {
    	//Singleton
        instance = this;
        
        //Logging
        logger = container.logger();
        
        logger.info("Planetside 2 Extended Push API v" + MavenInfo.getVersion());
        logger.info("Starting up...");
        
        //Config
        config = new Config();
        
        //Static Data
        new StaticDataManager();
        
        //Managers and Handlers
        dynamicDataManager = new DynamicDataManager();
        queryManager = new QueryManager();
        populationManager = new PopulationManager();
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

	public PopulationManager getPopulationManager()
	{
		return populationManager;
	}
}
