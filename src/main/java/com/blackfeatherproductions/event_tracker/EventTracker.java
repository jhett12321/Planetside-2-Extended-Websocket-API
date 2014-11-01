package com.blackfeatherproductions.event_tracker;

import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.blackfeatherproductions.event_tracker.game_data.GameData;
 
public class EventTracker extends Verticle
{
    //Singleton
    public static EventTracker instance;
    
    //Vertx
    private Logger logger;
    
    //Event Tracker Data
    private Config config;
    private EventHandler eventHandler;
    private GameData gameData;
    private QueryManager queryManager;
    private Utils utils;
    
    @Override
    public void start()
    {
        instance = this; //Singleton
        
        //Vertx
        logger = container.logger();
        
        //Event Tracker Data
        config = new Config();
        eventHandler = new EventHandler();
        gameData = new GameData();
        queryManager = new QueryManager();
        utils = new Utils();
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
