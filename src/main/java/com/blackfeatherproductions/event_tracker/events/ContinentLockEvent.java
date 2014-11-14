package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.game_data.Faction;
import com.blackfeatherproductions.event_tracker.game_data.MetagameEventType;
import com.blackfeatherproductions.event_tracker.game_data.World;
import com.blackfeatherproductions.event_tracker.game_data.Zone;

@EventInfo(eventNames = "ContinentLock")
public class ContinentLockEvent implements Event
{
	DataManager dataManager = EventTracker.getInstance().getDataManager();
	
	private String vs_population;
	private String nc_population;
	private String tr_population;
	
	private Faction triggering_faction;
	private Faction previous_faction;
	
	private MetagameEventType metagame_event;
	
	private Zone zone;
	private World world;
	
	private String timestamp;
	
	@Override
	public void preProcessEvent(JsonObject payload)
	{	
		this.timestamp = payload.getString("timestamp");
		
		this.vs_population = payload.getString("vs_population");
		this.nc_population = payload.getString("nc_population");
		this.tr_population = payload.getString("tr_population");
		
		this.triggering_faction = EventTracker.getInstance().getGameData().getFactionByID(payload.getString("triggering_faction"));
		this.previous_faction = EventTracker.getInstance().getGameData().getFactionByID(payload.getString("previous_faction"));
		
		this.zone = EventTracker.getInstance().getGameData().getZoneByID(payload.getString("zone_id"));
		this.world = EventTracker.getInstance().getGameData().getWorldByID(payload.getString("world_id"));
		this.metagame_event = EventTracker.getInstance().getGameData().getMetagameEventTypeByID(payload.getString("metagame_event_id"));
		
		processEvent();
	}

	@Override
	public void processEvent()
	{
		// TODO Auto-generated method stub
	}
}
