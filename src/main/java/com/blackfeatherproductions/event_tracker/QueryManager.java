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
	protected void getCensusData(final String query, boolean allowNoData, final int failureCount, final Query callback)
	{
		Vertx vertx = EventTracker.getInstance().getVertx();
		final Logger logger = EventTracker.getInstance().getLogger();
		HttpClient client = vertx.createHttpClient().setHost("foo.com");

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
		            		}
		            	}
		            	catch(DecodeException e)
		            	{
		            		//No Valid JSON was returned
		            		logger.warn("[Census Connection Error] - A census request returned invalid JSON. Retrying request...");
		            		logger.debug("Request: " + query);
		            		logger.debug(e.getMessage());
		            	}
		            }
		        });
		        
		        resp.exceptionHandler(new Handler<Throwable>()
		        {
					@Override
					public void handle(Throwable arg0)
					{
						
					}
		        });
		    }
		});
	}
}
