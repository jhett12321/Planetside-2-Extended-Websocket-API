package com.blackfeatherproductions.event_tracker;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;

import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.MetagameEventInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_static.World;

public class DynamicDataManager
{
	private Map<String, CharacterInfo> characters = new ConcurrentHashMap<String, CharacterInfo>();
	private Map<World, WorldInfo> worlds = new HashMap<World, WorldInfo>();
	
	public DynamicDataManager()
	{
        Vertx vertx = EventTracker.getInstance().getVertx();
        
        for(World world : World.worlds.values())
        {
        	worlds.put(world, new WorldInfo());
        }
        
        //Clears the character cache periodically
        vertx.setPeriodic(60000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	characters.clear();
            }
        });
        
        //Checks that any current metagame events should no-longer be running.
        vertx.setPeriodic(120000, new Handler<Long>()
        {
            public void handle(Long timerID)
            {
            	for(Entry<World,WorldInfo> worldInfo : worlds.entrySet())
            	{
            		for(MetagameEventInfo metagameEvent : worldInfo.getValue().getActiveMetagameEvents().values())
            		{
            			Date endTime = new Date(Long.valueOf(metagameEvent.getEndTime()) * 1000);
            			
            			if(new Date().after(endTime))
            			{
            				//TODO 1.1 (If Required) Trigger End Alert Event for overdue alerts.
            				EventTracker.getInstance().getLogger().warn("[WARNING] Alert ID " + metagameEvent.getInstanceID() + " on " + worldInfo.getKey().getName() + " is overdue.");
            			}
            		}
            	}
            }
        });
	}
	
	//Character Data
	public boolean characterDataExists(String characterID)
	{
		return characters.containsKey(characterID);
	}
	
	public CharacterInfo getCharacterData(String characterID)
	{
		return characters.get(characterID);
	}
	
	public void addCharacterData(String characterID, CharacterInfo character)
	{
		characters.put(characterID, character);
	}
	
	//World Data
	public WorldInfo getWorldInfo(World world)
	{
		if(world != null)
		{
			if(worlds.get(world) == null)
			{
				worlds.put(world, new WorldInfo());
			}
			
			return worlds.get(world);
		}
		
		return null;
	}
}
