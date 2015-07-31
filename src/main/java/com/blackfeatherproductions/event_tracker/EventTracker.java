package com.blackfeatherproductions.event_tracker;

import java.util.function.Consumer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import com.blackfeatherproductions.event_tracker.events.extended.population.PopulationManager;
import com.blackfeatherproductions.event_tracker.feeds.Census;
import com.blackfeatherproductions.event_tracker.feeds.CensusPS4EU;
import com.blackfeatherproductions.event_tracker.feeds.CensusPS4US;
import com.blackfeatherproductions.event_tracker.feeds.CensusRest;
import com.blackfeatherproductions.event_tracker.server.EventServer;

public class EventTracker extends AbstractVerticle
{
    //Instance
    public static EventTracker inst;
    
    //Logger
    private static Logger logger = LoggerFactory.getLogger(io.vertx.core.logging.JULLogDelegateFactory.class);

    //Data
    private static Config config;

    //Managers
    private static EventManager eventManager;
    private static DynamicDataManager dynamicDataManager;
    private static QueryManager queryManager;
    private static PopulationManager populationManager;
    
    //Feeds
    private static Census census;
    private static CensusPS4US censusPS4US;
    private static CensusPS4EU censusPS4EU;
    private static CensusRest censusRest;

    //Event Server
    private static EventServer eventServer;

    public static void main(String[] args)
    {
        Consumer<Vertx> runner = vertx -> {
            try
            {
                vertx.deployVerticle(EventTracker.class.getName());
            }
            catch (Throwable t)
            {
              t.printStackTrace();
            }
        };
        
        Vertx vertx = Vertx.vertx();
        runner.accept(vertx);
        
    }
    
    @Override
    public void start()
    {
        inst = this;
        
        //Logging
        logger.info("Planetside 2 Extended Push API v" + MavenInfo.getVersion());
        logger.info("Starting up...");

        //Config
        config = new Config();

        //Static Data
        StaticDataManager.Init();

        //Managers and Handlers
        dynamicDataManager = new DynamicDataManager();
        queryManager = new QueryManager();
        populationManager = new PopulationManager();
        eventManager = new EventManager();

        //Event Server
        eventServer = new EventServer();

        //Feeds
        census = new Census();
        censusPS4US = new CensusPS4US();
        censusPS4EU = new CensusPS4EU();
        censusRest = new CensusRest();
    }

    public static QueryManager getQueryManager()
    {
        return queryManager;
    }

    public static DynamicDataManager getDynamicDataManager()
    {
        return dynamicDataManager;
    }

    public static EventServer getEventServer()
    {
        return eventServer;
    }

    public static Logger getLogger()
    {
        return logger;
    }

    public static Config getConfig()
    {
        return config;
    }

    public static EventManager getEventHandler()
    {
        return eventManager;
    }

    public static PopulationManager getPopulationManager()
    {
        return populationManager;
    }
}
