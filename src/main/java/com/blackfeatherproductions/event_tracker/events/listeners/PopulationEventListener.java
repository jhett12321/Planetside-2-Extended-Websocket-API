package com.blackfeatherproductions.event_tracker.events.listeners;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.QueryManager;
import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.events.EventType;
import com.blackfeatherproductions.event_tracker.events.extended.population.PopulationManager;
import com.blackfeatherproductions.event_tracker.utils.CensusUtils;

@EventInfo(eventType = EventType.LISTENER,
        eventName = "PopulationEventListener",
        listenedEvents = "AchievementEarned|BattleRankUp|Death|DirectiveCompleted|PlayerLogin|PlayerLogout|VehicleDestroy",
        priority = EventPriority.NORMAL)
public class PopulationEventListener implements Event
{
    //Utils
    private final DynamicDataManager dynamicDataManager = EventTracker.instance.getDynamicDataManager();
    private final PopulationManager populationManager = EventTracker.instance.getPopulationManager();
    private final QueryManager queryManager = EventTracker.instance.getQueryManager();

    //Raw Data
    private String attackerCharacterID;
    private String characterID;
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
            attackerCharacterID = payload.getString("attacker_character_id");
            characterID = payload.getString("character_id");

            if (!CensusUtils.isValidCharacter(characterID))
            {
                characterID = attackerCharacterID;
            }

            if (!CensusUtils.isValidCharacter(attackerCharacterID))
            {
                attackerCharacterID = characterID;
            }

            if (dynamicDataManager.characterDataExists(attackerCharacterID) && dynamicDataManager.characterDataExists(characterID))
            {
                processEvent();
            }

            else
            {
                List<String> characterIDs = new ArrayList<>();
                characterIDs.add(characterID);

                if (!attackerCharacterID.equals(characterID))
                {
                    characterIDs.add(attackerCharacterID);
                }

                queryManager.queryCharacter(characterIDs, environment, this);
            }
        }
    }

    @Override
    public void processEvent()
    {
        CharacterInfo character = dynamicDataManager.getCharacterData(characterID);

        String eventName = payload.getString("event_name");

        //Logout Event.
        if (eventName.equals("PlayerLogout"))
        {
            populationManager.characterOffline(environment, characterID);
        }
        else
        {
            //Vehicle/Combat Events
            if (eventName.equals("Death") || eventName.equals("VehicleDestroy"))
            {
                processAttackerCharacter();
            }

            //All Character Events
            Faction faction;

            if (payload.containsKey("loadout_id"))
            {
                faction = Faction.getFactionByLoadoutID(payload.getString("loadout_id"));
            }

            else if (payload.containsKey("faction_id"))
            {
                faction = Faction.getFactionByID(payload.getString("faction_id"));
            }

            else
            {
                faction = character.getFaction();
            }

            String outfitID = character.getOutfitID();
            Zone zone;

            if (payload.containsKey("zone_id"))
            {
                zone = Zone.getZoneByID(payload.getString("zone_id"));
            }

            else
            {
                zone = character.getZone();
            }

            if (zone == null)
            {
                zone = Zone.UNKNOWN;
            }

            World world;

            if (payload.containsKey("world_id"))
            {
                world = World.getWorldByID(payload.getString("world_id"));
            }
            else
            {
                world = character.getWorld();
            }

            populationManager.characterOnline(environment, characterID, faction, outfitID, zone, world);
        }
    }

    private void processAttackerCharacter()
    {
        CharacterInfo attacker_character = dynamicDataManager.getCharacterData(attackerCharacterID);

        Faction faction = Faction.getFactionByLoadoutID(payload.getString("attacker_loadout_id"));
        String outfitID = attacker_character.getOutfitID();
        Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
        World world = World.getWorldByID(payload.getString("world_id"));

        populationManager.characterOnline(environment, attackerCharacterID, faction, outfitID, zone, world);
    }
}
