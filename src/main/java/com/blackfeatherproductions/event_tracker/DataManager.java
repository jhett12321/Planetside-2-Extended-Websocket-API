package com.blackfeatherproductions.event_tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.blackfeatherproductions.event_tracker.data.Character;
import com.blackfeatherproductions.event_tracker.data.MetagameEvent;

public class DataManager
{
	private Map<String, Character> characterData = new ConcurrentHashMap<String,Character>();
	private List<MetagameEvent> activeMetagameEvents = new ArrayList<MetagameEvent>();
	
	public Map<String, Character> getCharacterData()
	{
		return characterData;
	}
}
