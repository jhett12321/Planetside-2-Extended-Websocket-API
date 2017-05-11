package com.blackfeatherproductions.event_tracker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.function.Consumer;

import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.feeds.EventFeed;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import com.blackfeatherproductions.event_tracker.events.extended.population.PopulationManager;
import com.blackfeatherproductions.event_tracker.server.EventServer;

public class EventTracker extends AbstractVerticle
{
    //Instance
    public static EventTracker instance;

    private Logger logger;
    private Config config;

    //Managers
    private EventManager eventManager;
    private DynamicDataManager dynamicDataManager;
    private QueryManager queryManager;
    private PopulationManager populationManager;
    
    //Feeds
    private EventFeed census;
    private EventFeed censusPS4US;
    private EventFeed censusPS4EU;
    private EventFeed censusRest;

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
        instance = this;
        
        //Move Existing Logs.
        try
        {
            File existingLog = new File("eventTracker.log");
            File renLog = new File("eventTracker_prev.log");

            if(existingLog.exists())
            {
                Files.move(existingLog.toPath(), renLog.toPath(), REPLACE_EXISTING);
            }
        }
        catch (IOException ex)
        {
        }
        
        logger = LoggerFactory.getLogger(io.vertx.core.logging.JULLogDelegateFactory.class);
        
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
        
        //Static Data (Retrieved)
        StaticDataManager.CensusInit();

        //Event Server
        eventServer = new EventServer();

        //Feeds
        census = new EventFeed(Environment.PC);
        censusPS4US = new EventFeed(Environment.PS4_US);
        censusPS4EU = new EventFeed(Environment.PS4_EU);
        //censusRest = new EventFeed();
    }

    public QueryManager getQueryManager()
    {
        return queryManager;
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

    public PopulationManager getPopulationManager()
    {
        return populationManager;
    }
}
