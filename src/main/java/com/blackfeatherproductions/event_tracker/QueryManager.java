package com.blackfeatherproductions.event_tracker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang.StringUtils;
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
import com.blackfeatherproductions.event_tracker.queries.Query;

public class QueryManager
{
	private Integer failureCount = 0;
	
	private Queue<CharacterQuery> queuedCharacterQueries = new LinkedList<CharacterQuery>();
	
	public QueryManager()
	{
		//Character Queue Processor
        EventTracker.getInstance().getVertx().setPeriodic(1000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	List<String> characters = new ArrayList<String>();
            	List<CharacterQuery> callbacks = new ArrayList<CharacterQuery>();
            	
            	for(int i=0; i<queuedCharacterQueries.size(); i++)
            	{
            		CharacterQuery characterQuery = queuedCharacterQueries.poll();
            		
            		characters.addAll(characterQuery.getCharacterIDs());
            		callbacks.add(characterQuery);
            		
            		if(characters.size() >= 150)
            		{
                		getCensusData("/get/ps2:v2/character?character_id=" + StringUtils.join(characters, ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                				false, new CharacterListQuery(callbacks));
                		
            			characters = new ArrayList<String>();
            			callbacks = new ArrayList<CharacterQuery>();
            		}
            	}
            	
            	if(!characters.isEmpty())
            	{
            		getCensusData("/get/ps2:v2/character?character_id=" + StringUtils.join(characters, ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
            				false, new CharacterListQuery(callbacks));
            	}
            }
        });
	}
	
	public void getCensusData(String rawQuery, final boolean allowNoData, final Query... callbacks)
	{	
		Vertx vertx = EventTracker.getInstance().getVertx();
		final Logger logger = EventTracker.getInstance().getLogger();
		
		if(failureCount >= EventTracker.getInstance().getConfig().getMaxFailures())
		{
			logger.error("[Census REST] Census Failure Limit Reached. Dropping event.");
			
			for(Query callback : callbacks)
			{
				callback.ReceiveData(null);
			}
			
			failureCount = 0;
			return;
		}
		
		HttpClient client = vertx.createHttpClient().setHost("census.soe.com");
		
		final String query = "/s:" + EventTracker.getInstance().getConfig().getSoeServiceID() + rawQuery;

		client.getNow(query, new Handler<HttpClientResponse>()
		{
		    public void handle(HttpClientResponse resp)
		    {
		        resp.bodyHandler(new Handler<Buffer>()
		        {
		            public void handle(Buffer body)
		            {
		            	try
		            	{
		            		JsonObject data = new JsonObject(body.toString());
		            		
		            		if(data != null && data.containsField("returned") && data.getInteger("returned") != 0)
		            		{
		            			for(Query callback : callbacks)
		            			{
		            				callback.ReceiveData(data);
		            			}
		            			
		            			failureCount = 0;
		            			return;
		            		}
		            	}
		            	catch(DecodeException e)
		            	{
		            		//No Valid JSON was returned
		            		logger.warn("[Census REST] - A census request returned invalid JSON. Retrying request...");
		            		logger.warn("Failed Query " + failureCount.toString() + "/" + EventTracker.getInstance().getConfig().getMaxFailures().toString());
		            		logger.warn("Request: " + query);
		            		logger.warn(e.getMessage());
		            		
		            		failureCount++;
		            		
		            		getCensusData(query, allowNoData, callbacks);
		            	}
		            }
		        });
		        
		        resp.exceptionHandler(new Handler<Throwable>()
		        {
					@Override
					public void handle(Throwable e)
					{
	            		logger.warn("[Census REST] - A census request returned invalid JSON. Retrying request...");
	            		logger.warn("Failed Query " + failureCount.toString() + "/" + EventTracker.getInstance().getConfig().getMaxFailures().toString());
	            		logger.warn("Request: " + query);
	            		logger.warn(e.getMessage());
	            		
	            		failureCount++;
	            		
	            		getCensusData(query, allowNoData, callbacks);
					}
		        });
		    }
		});
	}
	
	public void addCharacterQuery(CharacterQuery query)
	{
		this.queuedCharacterQueries.add(query);
	}
}
