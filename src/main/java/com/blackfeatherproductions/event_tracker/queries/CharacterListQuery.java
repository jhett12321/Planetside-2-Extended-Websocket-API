package com.blackfeatherproductions.event_tracker.queries;

import com.blackfeatherproductions.event_tracker.Environment;

import java.util.List;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CharacterListQuery implements Query
{
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();
    
    private List<CharacterQuery> callbacks;

    public CharacterListQuery(List<CharacterQuery> callbacks)
    {
        this.callbacks = callbacks;
    }

    @Override
    public void receiveData(JsonObject data, Environment environment)
    {
        if(data != null)
        {
            JsonArray characterList = data.getJsonArray("character_list");

            for (int i = 0; i < characterList.size(); i++)
            {
                JsonObject characterData = characterList.getJsonObject(i);
                String characterID = characterData.getString("character_id");

                dynamicDataManager.addCharacterData(characterID, characterData);
            }
        }

        for (CharacterQuery callback : callbacks)
        {
            for (String characterID : callback.getCharacterIDs())
            {
                //Census does not always have data for all characters.
                //Since the above iterator loops over returned data, and not the requested ids, it does not create these blank entries.
                //In this case, we create a blank character.
                if (!EventTracker.getDynamicDataManager().characterDataExists(characterID))
                {
                    EventTracker.getDynamicDataManager().addCharacterData(characterID, null);
                }
            }

            callback.getCallbackEvent().processEvent(); //Triggers the waiting events for processing.
        }
    }

}
