package com.blackfeatherproductions.event_tracker;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;

import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data.dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data.dynamic.WorldInfo;

public class DynamicDataManager
{
	private Map<String, CharacterInfo> characterData = new ConcurrentHashMap<String, CharacterInfo>();
	private Map<World, WorldInfo> worldData = new HashMap<World, WorldInfo>();
	
	public DynamicDataManager()
	{
        Vertx vertx = EventTracker.getInstance().getVertx();
        
        for(World world : World.worlds.values())
        {
        	worldData.put(world, new WorldInfo());
        }
        
        //Clears the character cache periodically
        vertx.setPeriodic(60000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	characterData.clear();
            }
        });
        
        //Checks that any current metagame events should no-longer be running.
        vertx.setPeriodic(120000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	for(Entry<World,WorldInfo> worldInfo : worldData.entrySet())
            	{
            		for(MetagameEventInfo metagameEvent : worldInfo.getValue().getActiveMetagameEvents().values())
            		{
            			Date endTime = new Date(Long.valueOf(metagameEvent.getEndTime()) * 1000);
            			
            			if(new Date().after(endTime))
            			{
            				//TODO 1.1 (If Required) Trigger End Alert Event for overdue alerts.
            				EventTracker.getInstance().getLogger().warn("Alert ID " + metagameEvent.getInstanceID() + " on " + worldInfo.getKey().getName() + " is overdue.");
            			}
            		}
            	}
            }
        });
	}
	
	//Character Data
	public boolean characterDataExists(String characterID)
	{
		return characterData.containsKey(characterID);
	}
	
	public CharacterInfo getCharacterData(String characterID)
	{
		return characterData.get(characterID);
	}
	
	public void addCharacterData(String characterID, CharacterInfo character)
	{
		characterData.put(characterID, character);
	}
	
	//World Data
	public WorldInfo getWorldData(World world)
	{
		return worldData.get(world);
	}
}
