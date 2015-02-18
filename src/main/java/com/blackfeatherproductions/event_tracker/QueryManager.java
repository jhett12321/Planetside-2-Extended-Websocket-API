package com.blackfeatherproductions.event_tracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	
	private Queue<CharacterQuery> queuedCharacterQueries = new ConcurrentLinkedQueue<CharacterQuery>();
	
	public QueryManager()
	{
		//Character Queue Processor
        EventTracker.getInstance().getVertx().setPeriodic(1000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	Map <String, List<CharacterQuery>> characterLists = new HashMap<String, List<CharacterQuery>>();
            	
            	List<String> characters = new ArrayList<String>();
            	List<CharacterQuery> callbacks = new ArrayList<CharacterQuery>();
            	
            	for(CharacterQuery characterQuery : queuedCharacterQueries)
            	{
            		characters.addAll(characterQuery.getCharacterIDs());
            		callbacks.add(characterQuery);
            		
            		queuedCharacterQueries.remove(characterQuery);
            		
            		if(characters.size() >= 150)
            		{
            			characterLists.put(StringUtils.join(characters, ","), callbacks);
            			characters.clear();
            			callbacks.clear();
            		}
            	}
            	
            	if(!characters.isEmpty())
            	{
            		characterLists.put(StringUtils.join(characters, ","), callbacks);
        			characters.clear();
        			callbacks.clear();
            	}
            	
            	for(Entry<String, List<CharacterQuery>> characterList : characterLists.entrySet())
            	{
            		List<Query> queryCallbacks = new ArrayList<Query>();
            		
            		queryCallbacks.add(new CharacterListQuery()); //Makes the Character Info Objects
            		queryCallbacks.addAll(characterList.getValue()); //Triggers the waiting events for processing.
            		
            		getCensusData("/get/ps2:v2/character?character_id=" + characterList.getKey() + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit",
            				false, queryCallbacks.toArray(new Query[]{}));
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
			logger.error("[ERROR] [Census REST] Census Failure Limit Reached. Dropping event.");
			
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
		            		
		            		if(data != null && data.getInteger("returned") != null && data.getInteger("returned") != 0)
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
		            		logger.warn("[WARNING] [Census REST] - A census request returned invalid JSON. Retrying request...");
		            		logger.warn("[WARNING] Failed Query " + failureCount.toString() + "/" + EventTracker.getInstance().getConfig().getMaxFailures().toString());
		            		logger.warn("[WARNING] Request: " + query);
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
	            		logger.warn("[WARNING] [Census REST] - A census request returned invalid JSON. Retrying request...");
	            		logger.warn("[WARNING] Failed Query " + failureCount.toString() + "/" + EventTracker.getInstance().getConfig().getMaxFailures().toString());
	            		logger.warn("[WARNING] Request: " + query);
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
