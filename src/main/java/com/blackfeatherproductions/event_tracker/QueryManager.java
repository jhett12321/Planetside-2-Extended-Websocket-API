package com.blackfeatherproductions.event_tracker;

import com.blackfeatherproductions.event_tracker.data_static.Environment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.queries.CensusQuery;
import com.blackfeatherproductions.event_tracker.queries.CharacterListQuery;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;
import com.blackfeatherproductions.event_tracker.queries.MetagameEventQuery;
import com.blackfeatherproductions.event_tracker.queries.Query;
import com.blackfeatherproductions.event_tracker.queries.QueryPriority;
import com.blackfeatherproductions.event_tracker.queries.QueryPriorityComparator;
import com.blackfeatherproductions.event_tracker.queries.WorldQuery;
import com.blackfeatherproductions.event_tracker.queries.static_data.StaticFacilityListQuery;

public class QueryManager
{
    //Utils
    private final Vertx vertx = EventTracker.instance.getVertx();
    private final Logger logger = EventTracker.instance.getLogger();
    private final Config config = EventTracker.instance.getConfig();

    //Character Queries
    private Queue<CharacterQuery> queuedCharacterQueries = new ConcurrentLinkedQueue<>();
    private Map<Environment, List<String>> envCharacters = new ConcurrentHashMap<>();
    private Map<Environment, List<CharacterQuery>> envCallbacks = new ConcurrentHashMap<>();

    //Queries
    private final Queue<CensusQuery> queuedQueries = new PriorityBlockingQueue<>(100, new QueryPriorityComparator());

    private final int maxQueriesPerCycle = 5;
    private final int charactersPerQuery = 150;

    //Http Client
    HttpClientOptions options = new HttpClientOptions()
            .setDefaultHost(config.getCensusHostname())
            .setKeepAlive(true)
            .setMaxPoolSize(30);
    HttpClient client;

    public QueryManager()
    {
        client = vertx.createHttpClient(options);

        for (Environment environment : Environment.getEnvironments())
        {
            envCharacters.put(environment, new ArrayList<>());
            envCallbacks.put(environment, new ArrayList<>());
        }

        //Query Processor
        EventTracker.instance.getVertx().setPeriodic(1000, id ->
        {
            //Grab whatever is in the character queues, and split them into 150 character chunks.
            for (int i = 0; i < queuedCharacterQueries.size(); i++)
            {
                CharacterQuery characterQuery = queuedCharacterQueries.poll();

                Environment environment = characterQuery.getEnvironment();

                for (String characterID : characterQuery.getCharacterIDs())
                {
                    if (!envCharacters.get(environment).contains(characterID))
                    {
                        envCharacters.get(environment).add(characterID);
                    }
                }

                envCallbacks.get(environment).add(characterQuery);

                if (envCharacters.get(characterQuery.getEnvironment()).size() >= charactersPerQuery)
                {
                    queryCensus("character?character_id=" + StringUtils.join(envCharacters.get(environment), ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                            QueryPriority.LOWEST, environment, true, true, new CharacterListQuery(envCallbacks.remove(environment)));

                    envCharacters.get(environment).clear();
                    envCallbacks.put(environment, new ArrayList<CharacterQuery>());
                }
            }

            for (Entry<Environment, List<CharacterQuery>> callbacks : envCallbacks.entrySet())
            {
                if (!callbacks.getValue().isEmpty())
                {
                    queryCensus("character?character_id=" + StringUtils.join(envCharacters.get(callbacks.getKey()), ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                            QueryPriority.LOWEST, callbacks.getKey(), true, true, new CharacterListQuery(envCallbacks.remove(callbacks.getKey())));

                    envCharacters.get(callbacks.getKey()).clear();
                    envCallbacks.put(callbacks.getKey(), new ArrayList<CharacterQuery>());
                }
            }

            //Process anything we have in the query queue.
            for (int i = 0; i < queuedQueries.size(); i++)
            {
                if(i > maxQueriesPerCycle)
                {
                    EventTracker.instance.getLogger().warn("Hit census query limit (" + maxQueriesPerCycle +") for this cycle! This is typically normal for startup.\n" +
                            "Current queue size is " + queuedQueries.size());
                    break;
                }
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
    
    public void queryFacilityStaticData()
    {
        for(Environment environment : Environment.getValidEnvironments())
        {
            //Facility/Regions
            queryCensus("map_region?c:limit=2000&c:join=facility_link^on:facility_id^to:facility_id_b^inject_at:connecting_links.inbound^list:1,facility_link^on:facility_id^to:facility_id_a^inject_at:connecting_links.outbound^list:1",
                    QueryPriority.HIGHEST, environment, false, false, new StaticFacilityListQuery());
        }
    }

    public void queryWorld(String worldID, Environment environment)
    {
        queryCensus("map?world_id=" + worldID + "&zone_ids=2,4,6,8&c:join=map_region^on:Regions.Row.RowData.RegionId^to:map_region_id^inject_at:map_region",
                QueryPriority.HIGHEST, environment, false, false, new WorldQuery(worldID));
    }

    public void queryWorldMetagameEvents(String worldID, Environment environment)
    {
        String timestamp = String.valueOf(new Date().getTime() / 1000 - 7201);

        queryCensus("world_event/?type=METAGAME&c:limit=100&c:lang=en&world_id=" + worldID + "&after=" + timestamp,
                QueryPriority.HIGH, environment, true, true, new MetagameEventQuery());
    }

    public void queryCensus(String rawQuery, QueryPriority priority, Environment environment, boolean allowFailure, boolean allowNoData, Query callback)
    {
        queuedQueries.add(new CensusQuery(rawQuery, priority, environment, allowFailure, allowNoData, callback));
    }

    private void getCensusData(final CensusQuery censusQuery)
    {
        if (censusQuery.isCompleted())
        {
            return;
        }

        final String query = "/s:" + EventTracker.instance.getConfig().getServiceID() + "/get/" + censusQuery.getEnvironment().censusEndpoint + "/" + censusQuery.getRawQuery();

        client.getNow(query, response ->
        {
            response.bodyHandler(body ->
            {
                if (censusQuery.isCompleted())
                {
                    return;
                }

                try
                {
                    JsonObject data = new JsonObject(body.toString());

                    if (data.containsKey("error"))
                    {
                        logger.warn("[Census REST] Census reported an error when attempting query.");
                        logger.warn("[Census REST] Query: " + query);
                        logger.warn("[Census REST] Error: " + data.getString("error"));

                        censusQueryFailed(censusQuery);
                    }

                    else if (data.containsKey("returned") && ((data.getInteger("returned") > 0 && !censusQuery.isNoDataAllowed()) || censusQuery.isNoDataAllowed()))
                    {
                        censusQuery.getCallback().receiveData(data, censusQuery.getEnvironment());
                    }

                    else
                    {
                        //No Data was returned
                        logger.warn("[Census REST] - A census query returned no data.");
                        logger.warn("Query: " + query);

                        censusQueryFailed(censusQuery);
                    }
                }
                catch (DecodeException e)
                {
                    //No Valid JSON was returned
                    logger.warn("[Census REST] A census query returned invalid JSON.");
                    logger.warn("[Census REST] Query: " + query);
                    logger.warn("[Census REST] Exception: " + e.getMessage());

                    censusQueryFailed(censusQuery);
                }
            });

            response.exceptionHandler(e ->
            {
                logger.warn("[Census REST] A census query resulted in an exception.");
                logger.warn("[Census REST] Query: " + query);
                logger.warn("[Census REST] Exception: " + e.getMessage());

                censusQueryFailed(censusQuery);
            });
        });
    }

    private void censusQueryFailed(CensusQuery censusQuery)
    {
        if (censusQuery.getFailureCount() >= EventTracker.instance.getConfig().getMaxFailures())
        {
            if(censusQuery.isFailureAllowed())
            {
                logger.error("[Census REST] Census Failure Limit Reached. Dropping query.");

                censusQuery.getCallback().receiveData(null, censusQuery.getEnvironment());
                censusQuery.setFailureCount(EventTracker.instance.getConfig().getMaxFailures());
            }
            
            else
            {
                int delay = EventTracker.instance.getConfig().getRequiredQueryFailureDelay();
                logger.error("[Census REST] Census Failure Limit Reached for query, but is a required query for operation. Delaying " + delay + "ms until next attempt.");
                
                EventTracker.instance.getVertx().setTimer(delay, id ->
                {
                    retryQuery(censusQuery);
                });
            }
        }

        else
        {
            censusQuery.incrementFailureCount();
            logger.warn("[Census REST] Failed Query " + String.valueOf(censusQuery.getFailureCount()) + "/" + EventTracker.instance.getConfig().getMaxFailures().toString());
            retryQuery(censusQuery);
        }
    }
    
    private void retryQuery(CensusQuery censusQuery)
    {
        logger.warn("[Census REST] Retrying query...");
        queuedQueries.add(censusQuery);
    }
}
