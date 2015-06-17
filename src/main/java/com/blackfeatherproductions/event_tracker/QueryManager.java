package com.blackfeatherproductions.event_tracker;

import com.blackfeatherproductions.event_tracker.events.Event;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import com.blackfeatherproductions.event_tracker.queries.CharacterListQuery;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;
import com.blackfeatherproductions.event_tracker.queries.MetagameEventQuery;
import com.blackfeatherproductions.event_tracker.queries.Query;
import com.blackfeatherproductions.event_tracker.queries.WorldQuery;
import java.util.Date;

//TODO Add all queries to a queue.
//TODO Queries will have 25 attempts to succeed before failing, unless they are a "must return" data query.
public class QueryManager
{
    private final EventTracker eventTracker = EventTracker.getInstance();

    private Integer failureCount = 0;

    private Queue<CharacterQuery> queuedCharacterQueries = new LinkedList<CharacterQuery>();

    public QueryManager()
    {
        //Character Queue Processor
        eventTracker.getVertx().setPeriodic(1000, new Handler<Long>()
        {
            @Override
            public void handle(Long timerID)
            {
                List<String> characters = new ArrayList<String>();
                List<CharacterQuery> callbacks = new ArrayList<CharacterQuery>();

                for (int i = 0; i < queuedCharacterQueries.size(); i++)
                {
                    CharacterQuery characterQuery = queuedCharacterQueries.poll();

                    characters.addAll(characterQuery.getCharacterIDs());
                    callbacks.add(characterQuery);

                    if (characters.size() >= 150)
                    {
                        getCensusData("/get/ps2:v2/character?character_id=" + StringUtils.join(characters, ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                                true, new CharacterListQuery(callbacks));

                        characters = new ArrayList<String>();
                        callbacks = new ArrayList<CharacterQuery>();
                    }
                }

                if (!characters.isEmpty())
                {
                    getCensusData("/get/ps2:v2/character?character_id=" + StringUtils.join(characters, ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                            true, new CharacterListQuery(callbacks));
                }
            }
        });
    }

    public void getCensusData(final String rawQuery, final boolean allowNoData, final Query... callbacks)
    {
        Vertx vertx = eventTracker.getVertx();
        final Logger logger = eventTracker.getLogger();

        if (failureCount > eventTracker.getConfig().getMaxFailures() && allowNoData)
        {
            logger.error("[Census REST] Census Failure Limit Reached. Dropping event.");

            for (Query callback : callbacks)
            {
                callback.receiveData(null);
            }

            failureCount = eventTracker.getConfig().getMaxFailures();
            return;
        }

        HttpClient client = vertx.createHttpClient().setHost("census.daybreakgames.com");
        final String query = "/s:" + eventTracker.getConfig().getSoeServiceID() + rawQuery;
        
        client.exceptionHandler(new Handler<Throwable>()
        {
            @Override
            public void handle(Throwable e)
            {
                logger.warn("[Census REST] - A census request returned invalid JSON. Retrying request...");
                logger.warn("Failed Query " + failureCount.toString() + "/" + eventTracker.getConfig().getMaxFailures().toString());
                logger.warn("Request: " + query);
                logger.warn(e.getMessage());

                failureCount++;

                getCensusData(rawQuery, allowNoData, callbacks);
            }
        });

        client.getNow(query, new Handler<HttpClientResponse>()
        {
            @Override
            public void handle(HttpClientResponse resp)
            {
                resp.bodyHandler(new Handler<Buffer>()
                {
                    @Override
                    public void handle(Buffer body)
                    {
                        try
                        {
                            JsonObject data = new JsonObject(body.toString());

                            if (data.containsField("returned") && data.getInteger("returned") != 0)
                            {
                                for (Query callback : callbacks)
                                {
                                    callback.receiveData(data);
                                }

                                failureCount = 0;
                            }
                        }
                        catch (DecodeException e)
                        {
                            //No Valid JSON was returned
                            logger.warn("[Census REST] - A census request returned invalid JSON. Retrying request...");
                            logger.warn("Failed Query " + failureCount.toString() + "/" + eventTracker.getConfig().getMaxFailures().toString());
                            logger.warn("Request: " + query);
                            logger.warn(e.getMessage());

                            failureCount++;

                            getCensusData(rawQuery, allowNoData, callbacks);
                        }
                    }
                });
            }
        });
    }
    
    public void queryCharacter(List<String> characterIDs, Event callbackEvent)
    {
        this.queuedCharacterQueries.add(new CharacterQuery(characterIDs, callbackEvent));
    }
    
    public void queryCharacter(String characterID, Event callbackEvent)
    {
        this.queuedCharacterQueries.add(new CharacterQuery(characterID, callbackEvent));
    }
    
    public void queryWorld(String worldID)
    {
        getCensusData("/get/ps2:v2/map?world_id=" + worldID + "&zone_ids=2,4,6,8&c:join=map_region^on:Regions.Row.RowData.RegionId^to:map_region_id^inject_at:map_region^show:facility_id'facility_name'facility_type'facility_type_id", false, new WorldQuery(worldID));
    }
    
    public void queryWorldMetagameEvents(String worldID)
    {
        String timestamp = String.valueOf(new Date().getTime() / 1000 - 7201);

        getCensusData("/get/ps2:v2/world_event/?type=METAGAME&c:limit=100&c:lang=en&world_id=" + worldID + "&after=" + timestamp, false, new MetagameEventQuery());
    }
}
