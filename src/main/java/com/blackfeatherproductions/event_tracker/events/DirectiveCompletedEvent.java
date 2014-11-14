package com.blackfeatherproductions.event_tracker.events;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data.Character;
import com.blackfeatherproductions.event_tracker.game_data.Faction;
import com.blackfeatherproductions.event_tracker.game_data.World;
import com.blackfeatherproductions.event_tracker.game_data.Zone;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventNames = "DirectiveCompleted")
public class DirectiveCompletedEvent implements Event
{
	DataManager dataManager = EventTracker.getInstance().getDataManager();
	
	private Character character;
	
	private Faction faction;
	private Zone zone;
	private World world;
	
	private String outfit_id;
	private String directive_id;
	private String timestamp;
	
	@Override
	public void preProcessEvent(JsonObject payload)
	{
		//TODO Determine if we have already sent the directive event.
		
		String characterID = payload.getString("character_id");
		
		this.directive_id = payload.getString("directive_id");
		this.timestamp = payload.getString("timestamp");
		
		this.zone = EventTracker.getInstance().getGameData().getZoneByID(payload.getString("zone_id"));
		this.world = EventTracker.getInstance().getGameData().getWorldByID(payload.getObject("characters_world").getString("world_id"));
		
		if(dataManager.getCharacterData().containsKey(characterID))
		{
			this.character = dataManager.getCharacterData().get(characterID);
			
			faction = character.getFaction();
			outfit_id = character.getOutfitID();
			
			processEvent();
		}
		
		else
		{
			new CharacterQuery(characterID, this);
		}
	}

	@Override
	public void processEvent()
	{
		// TODO Auto-generated method stub
		
	}
}
