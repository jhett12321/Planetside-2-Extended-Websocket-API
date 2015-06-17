package com.blackfeatherproductions.event_tracker.queries;

import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.events.Event;

public class CharacterQuery
{
    private final Event callbackEvent;

    private final List<String> characterIDs = new ArrayList<String>();

    public CharacterQuery(List<String> characterIDs, Event callbackEvent)
    {
        this.callbackEvent = callbackEvent;
        this.characterIDs.addAll(characterIDs);
    }

    public CharacterQuery(String characterID, Event callbackEvent)
    {
        this.callbackEvent = callbackEvent;
        this.characterIDs.add(characterID);
    }

    public Event getCallbackEvent()
    {
        return callbackEvent;
    }

    public List<String> getCharacterIDs()
    {
        return characterIDs;
    }
}
