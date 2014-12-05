package com.blackfeatherproductions.event_tracker;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;
import org.vertx.java.core.logging.Logger;

import com.blackfeatherproductions.event_tracker.queries.Query;

public class QueryManager
{
	private Integer failureCount = 0;
	
	public void getCensusData(String rawQuery, final boolean allowNoData, final Query callback)
	{	
		Vertx vertx = EventTracker.getInstance().getVertx();
		final Logger logger = EventTracker.getInstance().getLogger();
		
		if(failureCount >= EventTracker.getInstance().getConfig().getMaxFailures())
		{
			logger.error("[Census Connection Error] Census Failure Limit Reached. Dropping event.");
			callback.ReceiveData(null);
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
		            		JsonObject data = Json.decodeValue(body.toString(), JsonObject.class);
		            		
		            		if(data != null && data.getInteger("returned") != null && data.getInteger("returned") != 0)
		            		{
		            			callback.ReceiveData(data);
		            			failureCount = 0;
		            			return;
		            		}
		            	}
		            	catch(DecodeException e)
		            	{
		            		//No Valid JSON was returned
		            		logger.warn("[Census Connection Error] - A census request returned invalid JSON. Retrying request...");
		            		logger.warn("Failed Query " + failureCount.toString() + "/" + EventTracker.getInstance().getConfig().getMaxFailures().toString());
		            		logger.debug("Request: " + query);
		            		logger.debug(e.getMessage());
		            		
		            		getCensusData(query, allowNoData, callback);
		            	}
		            }
		        });
		        
		        resp.exceptionHandler(new Handler<Throwable>()
		        {
					@Override
					public void handle(Throwable e)
					{
	            		logger.warn("[Census Connection Error] - A census request returned invalid JSON. Retrying request...");
	            		logger.warn("Failed Query " + failureCount.toString() + "/" + EventTracker.getInstance().getConfig().getMaxFailures().toString());
	            		logger.debug("Request: " + query);
	            		logger.debug(e.getMessage());
	            		
	            		getCensusData(query, allowNoData, callback);
					}
		        });
		    }
		});
	}
}
