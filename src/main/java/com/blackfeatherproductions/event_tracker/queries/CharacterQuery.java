package com.blackfeatherproductions.event_tracker.queries;

import com.blackfeatherproductions.event_tracker.Environment;
import java.util.ArrayList;
import java.util.List;

import com.blackfeatherproductions.event_tracker.events.Event;

public class CharacterQuery
{
    private final Event callbackEvent;
    private final List<String> characterIDs = new ArrayList<String>();
    private final Environment environment;

    public CharacterQuery(List<String> characterIDs, Environment environment, Event callbackEvent)
    {
        this.callbackEvent = callbackEvent;
        this.characterIDs.addAll(characterIDs);
        this.environment = environment;
    }

    public CharacterQuery(String characterID, Environment environment, Event callbackEvent)
    {
        this.callbackEvent = callbackEvent;
        this.characterIDs.add(characterID);
        this.environment = environment;
    }

    public Event getCallbackEvent()
    {
        return callbackEvent;
    }

    public List<String> getCharacterIDs()
    {
        return characterIDs;
    }
    
    public Environment getEnvironment()
    {
        return environment;
    }
}
