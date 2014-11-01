package com.blackfeatherproductions.event_tracker;

import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.blackfeatherproductions.event_tracker.feeds.Census;
import com.blackfeatherproductions.event_tracker.feeds.CensusRest;
import com.blackfeatherproductions.event_tracker.game_data.GameData;
 
public class EventTracker extends Verticle
{
    //Singleton
    public static EventTracker instance;
    
    //Vertx
    private Logger logger;
    
    //Data
    private Config config;
    private GameData gameData;
    
    //Managers and Handlers
    private EventHandler eventHandler;
    private QueryManager queryManager;
    private Utils utils;
    
    @Override
    public void start()
    {
        instance = this; //Singleton
        
        //Vertx
        logger = container.logger();
        
        //Data
        config = new Config();
        gameData = new GameData();
        
        //Managers and Handlers
        eventHandler = new EventHandler();
        queryManager = new QueryManager();
        utils = new Utils();
        
        //Feeds
        new Census();
        new CensusRest();
    }
    
    public QueryManager getQueryManager()
    {
		return queryManager;
	}

	public Utils getUtils()
    {
        return utils;
    }

    public GameData getGameData()
    {
        return gameData;
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
