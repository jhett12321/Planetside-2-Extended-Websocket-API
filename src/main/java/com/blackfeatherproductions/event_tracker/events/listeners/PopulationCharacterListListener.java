package com.blackfeatherproductions.event_tracker.events.listeners;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.QueryManager;
import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.events.extended.population.PopulationManager;

@EventInfo(eventType = EventType.LISTENER,
        eventName = "PopulationCharacterListListener",
        listenedEvents = "CharacterList",
        priority = EventPriority.NORMAL)
public class PopulationCharacterListListener implements Event
{
    //Utils
    private final DynamicDataManager dynamicDataManager = EventTracker.instance.getDynamicDataManager();
    private final PopulationManager populationManager = EventTracker.instance.getPopulationManager();
    private final QueryManager queryManager = EventTracker.instance.getQueryManager();

    //Raw Data
    private int queriesRemaining;
    private List<String> characterList = new ArrayList<>();
    private JsonObject payload;

    //Message Data
    private JsonObject eventData = null;
    private JsonObject filterData = null;
    private Environment environment;

    @Override
    public Environment getEnvironment()
    {
        return environment;
    }

    @Override
    public JsonObject getEventData()
    {
        return eventData;
    }

    @Override
    public JsonObject getFilterData()
    {
        return filterData;
    }

    @Override
    public void preProcessEvent(JsonObject payload, Environment environment)
    {
        this.payload = payload;
        this.environment = environment;

        if (payload != null)
        {
            JsonArray rawCharacterList = payload.getJsonArray("recent_character_id_list");
            
            if(rawCharacterList != null)
            {
                characterList = rawCharacterList.getList();
                
                List<String> characters = new ArrayList<>();

                for (int i = 0; i < rawCharacterList.size(); i++)
                {
                    String characterID = rawCharacterList.getString(i);

                    characters.add(characterID);

                    if (characters.size() >= 20)
                    {
                        queriesRemaining++;
                        queryManager.queryCharacter(characters, environment, this);

                        characters = new ArrayList<>();
                    }
                }

                if (!characters.isEmpty())
                {
                    queriesRemaining++;
                    queryManager.queryCharacter(characters, environment, this);
                }
            }
        }
    }

    @Override
    public void processEvent()
    {
        queriesRemaining--;
        if(queriesRemaining == 0)
        {
            for (String characterID : characterList)
            {
                CharacterInfo character = dynamicDataManager.getCharacterData(characterID);
                
                if(character != null)
                {
                    populationManager.characterOnline(environment, characterID, character.getFaction(), character.getOutfitID(), character.getZone(), character.getWorld());
                }
            }
            
            populationManager.updateEnvironmentStatus(environment, true);
        }
    }
}
