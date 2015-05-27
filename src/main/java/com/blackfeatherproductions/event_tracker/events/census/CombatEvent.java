package com.blackfeatherproductions.event_tracker.events.census;

import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.events.Event;
import com.blackfeatherproductions.event_tracker.events.EventInfo;
import com.blackfeatherproductions.event_tracker.events.EventPriority;
import com.blackfeatherproductions.event_tracker.queries.CharacterQuery;

@EventInfo(eventName = "Combat",
        listenedEvents = "Death",
        priority = EventPriority.NORMAL,
        filters =
        {
            "characters", "outfits", "factions", "loadouts", "vehicles", "weapons", "headshots", "zones", "worlds"
        })
public class CombatEvent implements Event
{
    private final EventTracker eventTracker = EventTracker.getInstance();
    private final DynamicDataManager dynamicDataManager = eventTracker.getDynamicDataManager();

    private JsonObject payload;

    //Preprocess Info
    private String attackerCharacterID;
    private String victimCharacterID;

    @Override
    public void preProcessEvent(JsonObject payload)
    {
        this.payload = payload;
        if (payload != null)
        {
            attackerCharacterID = payload.getString("attacker_character_id");
            victimCharacterID = payload.getString("character_id");

            if (!Utils.isValidCharacter(victimCharacterID))
            {
                victimCharacterID = attackerCharacterID;
            }

            if (!Utils.isValidCharacter(attackerCharacterID))
            {
                attackerCharacterID = victimCharacterID;
            }

            if (dynamicDataManager.characterDataExists(attackerCharacterID) && dynamicDataManager.characterDataExists(victimCharacterID))
            {
                processEvent();
            }

            else
            {
                List<String> characterIDs = new ArrayList<String>();
                characterIDs.add(victimCharacterID);

                if (!attackerCharacterID.equals(victimCharacterID))
                {
                    characterIDs.add(attackerCharacterID);
                }

                new CharacterQuery(characterIDs, this);
            }
        }
    }

    @Override
    public void processEvent()
    {
        //Data
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

        //Payload
        JsonObject eventData = new JsonObject();

        eventData.putString("attacker_character_id", attacker_character.getCharacterID());
        eventData.putString("attacker_character_name", attacker_character.getCharacterName());
        eventData.putString("attacker_outfit_id", attacker_outfit_id);
        eventData.putString("attacker_faction_id", attacker_faction.getID());
        eventData.putString("attacker_loadout_id", attacker_loadout_id);

        eventData.putString("victim_character_id", victim_character.getCharacterID());
        eventData.putString("victim_character_name", victim_character.getCharacterName());
        eventData.putString("victim_outfit_id", victim_outfit_id);
        eventData.putString("victim_faction_id", victim_faction.getID());
        eventData.putString("victim_loadout_id", victim_loadout_id);

        eventData.putString("weapon_id", weapon_id);
        eventData.putString("fire_mode_id", fire_mode_id);
        eventData.putString("vehicle_id", vehicle_id);
        eventData.putString("is_headshot", is_headshot);

        eventData.putString("timestamp", timestamp);
        eventData.putString("zone_id", zone.getID());
        eventData.putString("world_id", world.getID());

        //Filters
        JsonObject filterData = new JsonObject();

        filterData.putArray("characters", new JsonArray().addString(attacker_character.getCharacterID()).addString(victim_character.getCharacterID()));
        filterData.putArray("outfits", new JsonArray().addString(attacker_outfit_id).addString(victim_outfit_id));
        filterData.putArray("factions", new JsonArray().addString(attacker_faction.getID()).addString(victim_faction.getID()));
        filterData.putArray("loadouts", new JsonArray().addString(attacker_loadout_id).addString(victim_loadout_id));
        filterData.putArray("vehicles", new JsonArray().addString(vehicle_id));
        filterData.putArray("weapons", new JsonArray().addString(weapon_id));
        filterData.putArray("headshots", new JsonArray().addString(is_headshot));
        filterData.putArray("zones", new JsonArray().addString(zone.getID()));
        filterData.putArray("worlds", new JsonArray().addString(world.getID()));

        //Broadcast Event
        JsonObject message = new JsonObject();

        message.putObject("event_data", eventData);
        message.putObject("filter_data", filterData);

        eventTracker.getEventServer().broadcastEvent(this.getClass(), message);
    }
}
