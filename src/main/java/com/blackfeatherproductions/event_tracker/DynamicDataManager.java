package com.blackfeatherproductions.event_tracker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;

import com.blackfeatherproductions.event_tracker.data.World;
import com.blackfeatherproductions.event_tracker.data.dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data.dynamic.WorldInfo;

public class DynamicDataManager
{
	private Map<String, CharacterInfo> characterData = new ConcurrentHashMap<String, CharacterInfo>();
	private Map<World, WorldInfo> worldData = new HashMap<World, WorldInfo>();
	
	public DynamicDataManager()
	{
        Vertx vertx = EventTracker.getInstance().getVertx();
        
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
            	characterData.clear();
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
	
	//World Data
	public WorldInfo getWorldData(World world)
	{
		return worldData.get(world);
	}
}
