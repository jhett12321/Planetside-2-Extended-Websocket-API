package com.blackfeatherproductions.event_tracker.events.census;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.Environment;
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
import com.blackfeatherproductions.event_tracker.utils.CensusUtils;

@EventInfo(eventType = EventType.EVENT,
        eventName = "Combat",
        listenedEvents = "Death",
        priority = EventPriority.NORMAL,
        filters =
        {
            "characters", "outfits", "factions", "loadouts", "vehicles", "weapons", "headshots", "zones", "worlds"
        })
public class CombatEvent implements Event
{
    //Utils
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();
    private final QueryManager queryManager = EventTracker.getQueryManager();

    //Raw Data
    private JsonObject payload;
    private String attackerCharacterID;
    private String victimCharacterID;

    //Message Data
    private JsonObject eventData = new JsonObject();
    private JsonObject filterData = new JsonObject();
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
            victimCharacterID = payload.getString("character_id");

            if (!CensusUtils.isValidCharacter(victimCharacterID))
            {
                victimCharacterID = attackerCharacterID;
            }

            if (!CensusUtils.isValidCharacter(attackerCharacterID))
            {
                attackerCharacterID = victimCharacterID;
            }

            if (dynamicDataManager.characterDataExists(attackerCharacterID) && dynamicDataManager.characterDataExists(victimCharacterID))
            {
                processEvent();
            }

            else
            {
                List<String> characterIDs = new ArrayList<>();
                characterIDs.add(victimCharacterID);

                if (!attackerCharacterID.equals(victimCharacterID))
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
        //Raw Data
        CharacterInfo attacker_character = dynamicDataManager.getCharacterData(attackerCharacterID);
        String attacker_outfit_id = attacker_character.getOutfitID();
        String attacker_loadout_id = payload.getString("attacker_loadout_id");
        Faction attacker_faction = Faction.getFactionByLoadoutID(attacker_loadout_id);

        CharacterInfo victim_character = dynamicDataManager.getCharacterData(victimCharacterID);
        String victim_outfit_id = victim_character.getOutfitID();
        String victim_loadout_id = payload.getString("character_loadout_id");
        Faction victim_faction = Faction.getFactionByLoadoutID(victim_loadout_id);

        String weapon_id = payload.getString("attacker_weapon_id");
        String fire_mode_id = payload.getString("attacker_fire_mode_id");
        String vehicle_id = payload.getString("attacker_vehicle_id");
        String is_headshot = payload.getString("is_headshot");

        String timestamp = payload.getString("timestamp");
        Zone zone = Zone.getZoneByID(payload.getString("zone_id"));
        World world = World.getWorldByID(payload.getString("world_id"));
        
        //Update Character Data
        if(attacker_character.getFaction() != attacker_faction)
        {
            attacker_character.setFaction(attacker_faction);
            dynamicDataManager.update(attackerCharacterID);
        }
        
        if(victim_character.getFaction() != victim_faction)
        {
            victim_character.setFaction(victim_faction);
            dynamicDataManager.update(victimCharacterID);
        }

        //Event Data
        eventData.put("attacker_character_id", attacker_character.getCharacterID());
        eventData.put("attacker_character_name", attacker_character.getCharacterName());
        eventData.put("attacker_outfit_id", attacker_outfit_id);
        eventData.put("attacker_faction_id", attacker_faction.getID());
        eventData.put("attacker_loadout_id", attacker_loadout_id);

        eventData.put("victim_character_id", victim_character.getCharacterID());
        eventData.put("victim_character_name", victim_character.getCharacterName());
        eventData.put("victim_outfit_id", victim_outfit_id);
        eventData.put("victim_faction_id", victim_faction.getID());
        eventData.put("victim_loadout_id", victim_loadout_id);

        eventData.put("weapon_id", weapon_id);
        eventData.put("fire_mode_id", fire_mode_id);
        eventData.put("vehicle_id", vehicle_id);
        eventData.put("is_headshot", is_headshot);

        eventData.put("timestamp", timestamp);
        eventData.put("zone_id", zone.getID());
        eventData.put("world_id", world.getID());

        //Filter Data
        filterData.put("characters", new JsonArray().add(attacker_character.getCharacterID()).add(victim_character.getCharacterID()));
        filterData.put("outfits", new JsonArray().add(attacker_outfit_id).add(victim_outfit_id));
        filterData.put("factions", new JsonArray().add(attacker_faction.getID()).add(victim_faction.getID()));
        filterData.put("loadouts", new JsonArray().add(attacker_loadout_id).add(victim_loadout_id));
        filterData.put("vehicles", new JsonArray().add(vehicle_id));
        filterData.put("weapons", new JsonArray().add(weapon_id));
        filterData.put("headshots", new JsonArray().add(is_headshot));
        filterData.put("zones", new JsonArray().add(zone.getID()));
        filterData.put("worlds", new JsonArray().add(world.getID()));

        //Broadcast Event
        EventTracker.getEventServer().broadcastEvent(this);
    }
}
