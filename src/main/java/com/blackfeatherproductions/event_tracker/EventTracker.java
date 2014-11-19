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
    private DataManager dataManager;
    private QueryManager queryManager;
    private Utils utils;

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
        dataManager = new DataManager();
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

    public DataManager getDataManager()
    {
        return dataManager;
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
