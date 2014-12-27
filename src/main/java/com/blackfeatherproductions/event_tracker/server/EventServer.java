package com.blackfeatherproductions.event_tracker.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.Config;
import com.blackfeatherproductions.event_tracker.EventTracker;

public class EventServer
{
    public Map<ServerWebSocket,String> clientConnections;
    
    public EventServer()
    {
        EventTracker eventTracker = EventTracker.getInstance();
        Config config = eventTracker.getConfig();
        Vertx vertx = eventTracker.getVertx();
        
        clientConnections = new HashMap<ServerWebSocket,String>();
        
        vertx.createHttpServer().websocketHandler(new Handler<ServerWebSocket>()
        {
            public void handle(final ServerWebSocket ws)
            {
                //if (ws.path().equals("/apikey")) //TODO Verify API key
                //{
                    ws.dataHandler(new Handler<Buffer>()
                    {
                        public void handle(Buffer data)
                        {
                        	clientConnections.put(ws, null);
                            //TODO Initialize client subscriptions, send connection success message.
                        }
                    });
                //}
                //else
                //{
                //    ws.reject();
                //}
            }
        }).listen(config.getServerPort());
    }
    
    public void BroadcastEvent(JsonObject rawData)
    {
        JsonObject messageToSend = new JsonObject();
        
        messageToSend.putObject("payload", rawData.getObject("event_data"));
        messageToSend.putObject("event_type", rawData.getObject("event_type"));
        
        for(Entry<ServerWebSocket, String> connection : clientConnections.entrySet())
        {
            //TODO Filter events
            connection.getKey().writeTextFrame(messageToSend.encode());
        }
    }
}
