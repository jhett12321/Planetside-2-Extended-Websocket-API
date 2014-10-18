package com.blackfeatherproductions.EventTracker.feeds;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.EventTracker.Config;
import com.blackfeatherproductions.EventTracker.EventTracker;

public class Census
{
    public Census()
    {
        final EventTracker eventTracker = EventTracker.inst;
        Config config = eventTracker.getConfig();
        final Vertx vertx = eventTracker.getVertx();
        
        vertx.createHttpClient().connectWebsocket("wss://push.planetside2.com/streaming?service-id=s:" + config.getSoeServiceID(), new Handler<WebSocket>()
        {
            @Override
            public void handle(WebSocket websocket)
            {
                websocket.dataHandler(new Handler<Buffer>()
                {
                    @Override
                    public void handle(Buffer data)
                    {
                        JsonObject message = new JsonObject(data.toString());
                        if(message != null)
                        {
                            String serviceType = message.getString("type");
                            if(serviceType != null && serviceType == "serviceStateChanged")
                            {
                                eventTracker.getLogger().info("Received Census Server State Message: " + message.encode());
                            }
                            
                            else if(serviceType != null && serviceType == "serviceMessage")
                            {
                                JsonObject payload = message.getObject("payload");
                                String eventName = payload.getString("event_name");
                                
                                eventTracker.getEventHandler().handleEvent(eventName, payload);
                            }
                        }
                    }
                });
                
                //Send subscription message
                websocket.writeTextFrame("{\"service\": \"event\",\"action\": \"subscribe\",\"characters\": [\"all\"],\"worlds\": [\"all\"],\"eventNames\": [\"Death\",\"FacilityControl\",\"MetagameEvent\",\"PlayerLogin\",\"PlayerLogout\",\"VehicleDestroy\",\"BattleRankUp\",\"AchievementEarned\"]}");
            }
        });
    }
}
