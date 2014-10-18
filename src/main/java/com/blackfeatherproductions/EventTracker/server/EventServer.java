package com.blackfeatherproductions.EventTracker.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;
import com.blackfeatherproductions.EventTracker.Config;
import com.blackfeatherproductions.EventTracker.EventTracker;

public class EventServer
{
    public Map<ServerWebSocket,Subscriptions> clientConnections;
    
    public EventServer()
    {
        EventTracker eventTracker = EventTracker.inst;
        Config config = eventTracker.getConfig();
        Vertx vertx = eventTracker.getVertx();
        
        clientConnections = new HashMap<ServerWebSocket,Subscriptions>();
        
        vertx.createHttpServer().websocketHandler(new Handler<ServerWebSocket>()
        {
            public void handle(final ServerWebSocket ws)
            {
                if (ws.path().equals("/apikey")) //TODO Verify API key
                {
                    ws.dataHandler(new Handler<Buffer>()
                    {
                        public void handle(Buffer data)
                        {
                            //TODO Initialize client subscriptions, send connection success message.
                        }
                    });
                }
                else
                {
                    ws.reject();
                }
            }
        }).listen(config.getServerPort());
    }
    
    public void BroadcastEvent(JsonObject rawData)
    {
        JsonObject messageToSend = new JsonObject();
        
        messageToSend.putObject("payload", rawData.getObject("messageData"));
        messageToSend.putObject("event_type", rawData.getObject("eventType"));
        
        for(Entry<ServerWebSocket, Subscriptions> connection : clientConnections.entrySet())
        {
            //TODO Filter events
            connection.getKey().writeTextFrame(messageToSend.encode());
        }
    }
}
