package com.blackfeatherproductions.event_tracker;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import com.blackfeatherproductions.event_tracker.events.extended.population.PopulationManager;
import com.blackfeatherproductions.event_tracker.feeds.Census;
import com.blackfeatherproductions.event_tracker.feeds.CensusPS4EU;
import com.blackfeatherproductions.event_tracker.feeds.CensusPS4US;
import com.blackfeatherproductions.event_tracker.feeds.CensusRest;
import com.blackfeatherproductions.event_tracker.server.EventServer;

public class EventTracker
{
    //Vertx
    private static Vertx vertx = Vertx.vertx();
    private static Logger logger = LoggerFactory.getLogger(io.vertx.core.logging.JULLogDelegateFactory.class);

    //Data
    private static Config config;

    //Managers
    private static EventManager eventManager;
    private static DynamicDataManager dynamicDataManager;
    private static QueryManager queryManager;
    private static PopulationManager populationManager;

    //Event Server
    private static EventServer eventServer;

    public static void main(String[] args)
    {
        //Logging
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
        eventManager = new EventManager();

        //Event Server
        eventServer = new EventServer();

        //Feeds
        new Census();
        new CensusPS4US();
        new CensusPS4EU();
        new CensusRest();
    }

    public static Vertx getVertx()
    {
        return vertx;
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
