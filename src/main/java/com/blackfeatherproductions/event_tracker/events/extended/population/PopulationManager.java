package com.blackfeatherproductions.event_tracker.events.extended.population;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.QueryManager;
import com.blackfeatherproductions.event_tracker.data_dynamic.CharacterInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.OnlinePlayer;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.Environment;
import com.blackfeatherproductions.event_tracker.queries.Query;
import com.blackfeatherproductions.event_tracker.queries.QueryPriority;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;

public class PopulationManager implements Query
{
    //Utils
    private final DynamicDataManager dynamicDataManager = EventTracker.getDynamicDataManager();
    private final QueryManager queryManager = EventTracker.getQueryManager();

    //Online Characters
    private Map<Environment, Map<String, OnlinePlayer>> envOnlinePlayers = new EnumMap<>(Environment.class);
    
    //Characters that have passed the dirty stage.
    private Map<Environment, List<String>> envCharactersToCheck = new EnumMap<>(Environment.class);
    
    //Population Data
    private Map<Environment, PopulationStore> envTotalPopulations = new EnumMap<>(Environment.class);
    private Map<Environment, Map<World, PopulationStore>> envWorldPopulations = new EnumMap<>(Environment.class);
    private Map<Environment, Map<String, PopulationStore>> envZonePopulations = new EnumMap<>(Environment.class);
    
    //Instance used for query manager.
    private Query self = this;

    public PopulationManager()
    {
        Vertx vertx = EventTracker.getVertx();
        
        //Create Population Stores
        for(Environment environment : Environment.values())
        {
            //Online Characters
            envOnlinePlayers.put(environment, new ConcurrentHashMap<>());
            
            //Total Population
            envTotalPopulations.put(environment, new PopulationStore());
            
            //World & Zone Population
            Map<World, PopulationStore> worldPopulations = new HashMap<>();
            Map<String, PopulationStore> zonePopulations = new HashMap<>();

            for (World world : World.getAllWorlds())
            {
                worldPopulations.put(world, new PopulationStore());

                for (Zone zone : Zone.getAllZones())
                {
                    zonePopulations.put(world.getID() + "_" + zone.getID(), new PopulationStore());
                }
            }
            
            envWorldPopulations.put(environment, worldPopulations);
            envZonePopulations.put(environment, zonePopulations);
            
            //Character Checking List
            envCharactersToCheck.put(environment, new ArrayList<>());
        }

        //Detects if players are no-longer online (i.e. we missed the logout event.)
        vertx.setPeriodic(600000, id ->
        {
            //Check players have not logged off on all environments.
            for(Entry<Environment, List<String>> charactersToCheckEntry : envCharactersToCheck.entrySet())
            {
                Environment environment = charactersToCheckEntry.getKey();
                List<String> charactersToCheck = charactersToCheckEntry.getValue();

                //Remove characters that census failed to retrieve online statuses for (deleted characters, low BR, etc)
                for (String characterID : charactersToCheck)
                {
                    if (!dynamicDataManager.characterDataExists(characterID) || !dynamicDataManager.getCharacterData(characterID).isOnline())
                    {
                        characterOffline(environment, characterID);
                    }
                }

                charactersToCheck.clear();

                Iterator<Map.Entry<String, OnlinePlayer>> iter = envOnlinePlayers.get(environment).entrySet().iterator();

                while (iter.hasNext())
                {
                    Entry<String, OnlinePlayer> entry = iter.next();

                    if ((new Date().getTime() - entry.getValue().getLastEvent().getTime()) / 1000 > 600)
                    {
                        charactersToCheck.add(entry.getKey());
                    }
                }

                List<String> characters = new ArrayList<>();

                for (String characterID : charactersToCheck)
                {
                    characters.add(characterID);

                    if (characters.size() >= 150)
                    {
                        queryManager.queryCensus("character?character_id=" + StringUtils.join(characters, ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                            QueryPriority.LOW, environment, true, true, self);

                        characters.clear();
                    }
                }

                if (!characters.isEmpty())
                {
                    queryManager.queryCensus("character?character_id=" + StringUtils.join(characters, ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                        QueryPriority.LOW, environment, true, true, self);
                }
            }
        });
    }

    public void characterOnline(Environment environment, String characterID, Faction faction, String outfitID, Zone zone, World world)
    {
        boolean modified = false;
        boolean zoneChangeOnly = true;
        
        OnlinePlayer player;
        
        if (envOnlinePlayers.get(environment).containsKey(characterID))
        {
            player = envOnlinePlayers.get(environment).get(characterID);
            player.setLastEvent(new Date());
            
            if(player.getFaction() != faction)
            {
                modified = true;
                zoneChangeOnly = false;
            }
            
            if(!player.getOutfitID().equals(outfitID))
            {
                modified = true;
                zoneChangeOnly = false;
            }
            
            if(player.getZone() != zone)
            {
                modified = true;
            }
            
            if(player.getWorld() != world)
            {
                modified = true;
                zoneChangeOnly = false;
            }
        }

        else
        {
            //Create a new Online Character
            player = new OnlinePlayer(faction, outfitID, zone, world);
            envOnlinePlayers.get(environment).put(characterID, player);
            
            modified = true;
            zoneChangeOnly = false;
        }
        
        if(modified)
        {
            //Decrement the relating stores for old data.
            if(!zoneChangeOnly)
            {
                //Total Populations
                envTotalPopulations.get(environment).decrementPopulation(player);

                //World Populations
                envWorldPopulations.get(environment).get(player.getWorld()).decrementPopulation(player);
            }
            
            //Zone Populations
            envZonePopulations.get(environment).get(player.getWorld().getID() + "_" + player.getZone().getID()).decrementPopulation(player);
            
            //Update player values
            player.setFaction(faction);
            player.setOutfitID(outfitID);
            player.setZone(zone);
            player.setWorld(world);
            
            //Increment the relating stores for this.
            if(!zoneChangeOnly)
            {
                //Total Populations
                envTotalPopulations.get(environment).incrementPopulation(player);

                //World Populations
                envWorldPopulations.get(environment).get(world).incrementPopulation(player);
            }
            
            //Zone Populations
            envZonePopulations.get(environment).get(world.getID() + "_" + zone.getID()).incrementPopulation(player);
            
            //Generate events
            generateEvents(environment, player, zoneChangeOnly);
        }
    }
    
    public void characterOffline(Environment environment, String characterID)
    {
        if (envOnlinePlayers.get(environment).containsKey(characterID))
        {
            //Removes and gets our player from the online players list.
            OnlinePlayer player = envOnlinePlayers.get(environment).remove(characterID);
            
            //Decrement the relating stores for this.
            //Total Populations
            envTotalPopulations.get(environment).decrementPopulation(player);
            
            //World Populations
            envWorldPopulations.get(environment).get(player.getWorld()).decrementPopulation(player);
            
            //Zone Populations
            envZonePopulations.get(environment).get(player.getWorld().getID() + "_" + player.getZone().getID()).decrementPopulation(player);
            
            //Generate events
            generateEvents(environment, player, false);
        }
    }
    
    private void generateEvents(Environment environment, OnlinePlayer player, boolean zoneChangeOnly)
    {
        String eventName = "PopulationChange";
        
        if(!zoneChangeOnly)
        {
            //Total Population
            PopulationStore totalPopulation = envTotalPopulations.get(environment);

            JsonObject totalPayload = new JsonObject();
            totalPayload.put("population_type", "total");
            totalPayload.put("population_total", totalPopulation.getTotalPopulation().toString());
            totalPayload.put("population_vs", totalPopulation.getFactionPopulation(Faction.VS).toString());
            totalPayload.put("population_nc", totalPopulation.getFactionPopulation(Faction.NC).toString());
            totalPayload.put("population_tr", totalPopulation.getFactionPopulation(Faction.TR).toString());
            totalPayload.put("population_unk", totalPopulation.getFactionPopulation(Faction.NS).toString());

            EventTracker.getEventHandler().handleEvent(eventName, totalPayload, environment);

            //World Populations
            PopulationStore worldPopulation = envWorldPopulations.get(environment).get(player.getWorld());
            JsonObject worldPayload = new JsonObject();

            worldPayload.put("population_type", "world");
            worldPayload.put("population_total", worldPopulation.getTotalPopulation().toString());
            worldPayload.put("population_vs", worldPopulation.getFactionPopulation(Faction.VS).toString());
            worldPayload.put("population_nc", worldPopulation.getFactionPopulation(Faction.NC).toString());
            worldPayload.put("population_tr", worldPopulation.getFactionPopulation(Faction.TR).toString());
            worldPayload.put("population_unk", worldPopulation.getFactionPopulation(Faction.NS).toString());
            worldPayload.put("world_id", player.getWorld().getID());

            EventTracker.getEventHandler().handleEvent(eventName, worldPayload, environment);
            
            //World Outfit
            JsonObject outfitPayload = new JsonObject();

            outfitPayload.put("population_type", "outfit");
            outfitPayload.put("population_total", worldPopulation.getOutfitPopulation(player.getOutfitID()).toString());
            outfitPayload.put("outfit_id", player.getOutfitID());
            outfitPayload.put("world_id", player.getWorld().getID());

            EventTracker.getEventHandler().handleEvent(eventName, outfitPayload, environment);
        }
        
        //Zone Populations
        PopulationStore zonePopulation = envZonePopulations.get(environment).get(player.getWorld().getID() + "_" + player.getZone().getID());
        JsonObject zonePayload = new JsonObject();

        zonePayload.put("population_type", "zone");
        zonePayload.put("population_total", zonePopulation.getTotalPopulation().toString());
        zonePayload.put("population_vs", zonePopulation.getFactionPopulation(Faction.VS).toString());
        zonePayload.put("population_nc", zonePopulation.getFactionPopulation(Faction.NC).toString());
        zonePayload.put("population_tr", zonePopulation.getFactionPopulation(Faction.TR).toString());
        zonePayload.put("population_unk", zonePopulation.getFactionPopulation(Faction.NS).toString());
        zonePayload.put("zone_id", player.getZone().getID());
        zonePayload.put("world_id", player.getWorld().getID());

        EventTracker.getEventHandler().handleEvent(eventName, zonePayload, environment);

        //Zone Outfit Populations
        JsonObject outfitZonePayload = new JsonObject();

        outfitZonePayload.put("population_type", "zone_outfit");
        outfitZonePayload.put("population_total", zonePopulation.getOutfitPopulation(player.getOutfitID()).toString());
        outfitZonePayload.put("outfit_id", player.getOutfitID());
        outfitZonePayload.put("zone_id", player.getZone().getID());
        outfitZonePayload.put("world_id", player.getWorld().getID());

        EventTracker.getEventHandler().handleEvent(eventName, outfitZonePayload, environment);
    }

    @Override
    public void receiveData(JsonObject data, Environment environment)
    {
        if(data != null)
        {
            JsonArray characterList = data.getJsonArray("character_list");

            for (int i = 0; i < characterList.size(); i++)
            {
                //Update Character Info
                JsonObject characterData = characterList.getJsonObject(i);

                String characterID = characterData.getString("character_id");
                dynamicDataManager.addCharacterData(characterID, characterData);

                //Verify Population Data.
                if (dynamicDataManager.characterDataExists(characterID))
                {
                    CharacterInfo characterInfo = dynamicDataManager.getCharacterData(characterID);

                    if (characterInfo.isOnline())
                    {
                        characterOnline(environment, characterID, characterInfo.getFaction(), characterInfo.getOutfitID(), characterInfo.getZone(), characterInfo.getWorld());
                    }

                    else
                    {
                        //This player doesn't have valid online status data.
                        //This player is no-longer online. Remove the player.
                        characterOffline(environment, characterID);
                    }
                }
            }
        }
    }
}
