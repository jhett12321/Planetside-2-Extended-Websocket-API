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
    private StaticDataManager staticDataManager;
	private DynamicDataManager dynamicDataManager;
    private QueryManager queryManager;
    private Utils utils;

    //Event Server
	private EventServer eventServer;
    
    @Override
    public void start()
    {
        instance = this; //Singleton
        
        //Vertx
        logger = container.logger();
        
        //Data
        config = new Config();
        
        //Managers and Handlers
        eventManager = new EventManager();
        queryManager = new QueryManager();
        staticDataManager = new StaticDataManager();
        dynamicDataManager = new DynamicDataManager();
        utils = new Utils();
        
        //Feeds
        new Census();
        new CensusRest();
        
        //Event Server
        eventServer = new EventServer();
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

    public StaticDataManager getStaticDataManager()
    {
        return staticDataManager;
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
}
