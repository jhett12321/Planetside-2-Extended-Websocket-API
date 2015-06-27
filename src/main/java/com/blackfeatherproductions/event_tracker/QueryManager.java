package com.blackfeatherproductions.event_tracker;

import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.queries.CensusQuery;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

import com.blackfeatherproductions.event_tracker.queries.CharacterListQuery;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;
import com.blackfeatherproductions.event_tracker.queries.MetagameEventQuery;
import com.blackfeatherproductions.event_tracker.queries.Query;
import com.blackfeatherproductions.event_tracker.queries.QueryPriority;
import com.blackfeatherproductions.event_tracker.queries.QueryPriorityComparator;
import com.blackfeatherproductions.event_tracker.queries.WorldQuery;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.PriorityBlockingQueue;

public class QueryManager
{
    //Utils
    private final Vertx vertx = EventTracker.getVertx();
    private final Logger logger = EventTracker.getLogger();

    private Integer failureCount = 0;

    //Character Queries
    private Queue<CharacterQuery> queuedCharacterQueries = new LinkedList<>();
    private Map<Environment, List<String>> envCharacters = new EnumMap<>(Environment.class);
    private Map<Environment, List<CharacterQuery>> envCallbacks = new EnumMap<>(Environment.class);
    
    //Queries
    private final Queue<CensusQuery> queuedQueries = new PriorityBlockingQueue<>(100, new QueryPriorityComparator());
    
    //Http Client
    HttpClientOptions options = new HttpClientOptions().setDefaultHost("census.daybreakgames.com").setKeepAlive(true);
    HttpClient client;

    public QueryManager()
    {
        client = vertx.createHttpClient(options);
        
        for(Environment environment : Environment.values())
        {
            envCharacters.put(environment, new ArrayList<String>());
            envCallbacks.put(environment, new ArrayList<CharacterQuery>());
        }
        
        //Query Processor
        EventTracker.getVertx().setPeriodic(1000, id ->
        {
            //Grab whatever is in the character queues, and split them into 150 character chunks.
            CharacterQuery characterQuery;
            while ((characterQuery = queuedCharacterQueries.poll()) != null)
            {
                Environment environment = characterQuery.getEnvironment();

                envCharacters.get(environment).addAll(characterQuery.getCharacterIDs());
                envCallbacks.get(environment).add(characterQuery);

                if(envCharacters.get(characterQuery.getEnvironment()).size() >= 150)
                {
                    queryCensus("character?character_id=" + StringUtils.join(envCharacters.get(environment), ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                        QueryPriority.LOWEST, environment, true, true, new CharacterListQuery(envCallbacks.get(environment)));

                    envCharacters.get(environment).clear();
                    envCallbacks.put(environment, new ArrayList<CharacterQuery>());
                }
            }

            for(Entry<Environment, List<CharacterQuery>> callbacks : envCallbacks.entrySet())
            {
                if(!callbacks.getValue().isEmpty())
                {
                    queryCensus("character?character_id=" + StringUtils.join(envCharacters.get(callbacks.getKey()), ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                        QueryPriority.LOWEST, callbacks.getKey(), true, true, new CharacterListQuery(envCallbacks.get(callbacks.getKey())));

                    envCharacters.get(callbacks.getKey()).clear();
                    envCallbacks.put(callbacks.getKey(), new ArrayList<CharacterQuery>());
                }
            }

            //Process anything we have in the query queue.
            for (int i = 0; i < queuedQueries.size(); i++)
            {
                getCensusData(queuedQueries.poll());
            }
        });
    }
    
    public void queryCharacter(List<String> characterIDs, Environment environment, Event callbackEvent)
    {
        this.queuedCharacterQueries.add(new CharacterQuery(characterIDs, environment, callbackEvent));
    }
    
    public void queryCharacter(String characterID, Environment environment, Event callbackEvent)
    {
        this.queuedCharacterQueries.add(new CharacterQuery(characterID, environment, callbackEvent));
    }
    
    public void queryWorld(String worldID, Environment environment)
    {
        queryCensus("map?world_id=" + worldID + "&zone_ids=2,4,6,8&c:join=map_region^on:Regions.Row.RowData.RegionId^to:map_region_id^inject_at:map_region^show:facility_id'facility_name'facility_type'facility_type_id",
                QueryPriority.HIGHEST, environment, false, false, new WorldQuery(worldID)); //Make allow no data false again.
    }
    
    public void queryWorldMetagameEvents(String worldID, Environment environment)
    {
        String timestamp = String.valueOf(new Date().getTime() / 1000 - 7201);

        queryCensus("world_event/?type=METAGAME&c:limit=100&c:lang=en&world_id=" + worldID + "&after=" + timestamp,
                QueryPriority.HIGH, environment, false, true, new MetagameEventQuery()); //Make allow no data false again.
    }

    public void queryCensus(String rawQuery, QueryPriority priority, Environment environment, boolean allowFailure, boolean allowNoData, Query... callbacks)
    {
        queuedQueries.add(new CensusQuery(rawQuery, priority, environment, allowFailure, allowNoData, callbacks));
    }
    
    private void getCensusData(final CensusQuery censusQuery)
    {
        if (failureCount > EventTracker.getConfig().getMaxFailures() && censusQuery.isFailureAllowed())
        {
            logger.error("[Census REST] Census Failure Limit Reached. Dropping event.");

            for (Query callback : censusQuery.getCallbacks())
            {
                callback.receiveData(null, censusQuery.getEnvironment());
            }

            failureCount = EventTracker.getConfig().getMaxFailures();
            return;
        }
        
        String queryPrefix;
        switch(censusQuery.getEnvironment())
        {
            case PC:
            {
                queryPrefix = "ps2:v2";
                break;
            }
            case PS4_US:
            {
                queryPrefix = "ps2ps4us:v2";
                break;
            }
            case PS4_EU:
            {
                queryPrefix = "ps2ps4eu:v2";
                break;
            }
            default:
            {
                EventTracker.getLogger().warn("[Census REST] - An unsupported environment was sent for querying. Ignoring request...");
                for (Query callback : censusQuery.getCallbacks())
                {
                    callback.receiveData(null, censusQuery.getEnvironment());
                }
                return;
            }
        }
        
        final String query = "/s:" + EventTracker.getConfig().getSoeServiceID() + "/get/" + queryPrefix + "/" + censusQuery.getRawQuery();

        client.getNow(query, response ->
        {
            response.bodyHandler(body ->
            {
                try
                {
                    JsonObject data = new JsonObject(body.toString());

                    if(data.containsKey("returned") && ((data.getInteger("returned") > 0 && !censusQuery.isNoDataAllowed()) || censusQuery.isNoDataAllowed()))
                    {
                        for (Query callback : censusQuery.getCallbacks())
                        {
                            callback.receiveData(data, censusQuery.getEnvironment());
                        }

                        failureCount = 0;
                    }

                    else
                    {
                        //No Data was returned
                        logger.warn("[Census REST] - A census request returned no data. Retrying request...");
                        logger.warn("Failed Query " + failureCount.toString() + "/" + EventTracker.getConfig().getMaxFailures().toString());
                        logger.warn("Request: " + query);

                        failureCount++;

                        queuedQueries.add(censusQuery);
                    }
                }
                catch (DecodeException e)
                {
                    //No Valid JSON was returned
                    logger.warn("[Census REST] - A census request returned invalid JSON. Retrying request...");
                    logger.warn("Failed Query " + failureCount.toString() + "/" + EventTracker.getConfig().getMaxFailures().toString());
                    logger.warn("Request: " + query);
                    logger.warn(e.getMessage());

                    failureCount++;

                    queuedQueries.add(censusQuery);
                }
            });
            
            response.exceptionHandler(e ->
            {
                logger.warn("[Census REST] - A census request resulted in an exception. Retrying request...");
                logger.warn("Failed Query " + failureCount.toString() + "/" + EventTracker.getConfig().getMaxFailures().toString());
                logger.warn("Request: " + query);
                logger.warn(e.getMessage());

                failureCount++;

                queuedQueries.add(censusQuery);
            });
        });
    }
}
