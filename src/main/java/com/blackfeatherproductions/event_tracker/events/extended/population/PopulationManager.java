package com.blackfeatherproductions.event_tracker.events.extended.population;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

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

public class PopulationManager implements Query
{
    private final EventTracker eventTracker = EventTracker.getInstance();
    private final DynamicDataManager dynamicDataManager = eventTracker.getDynamicDataManager();
    private final QueryManager queryManager = eventTracker.getQueryManager();

    public Map<Environment, Map<String, OnlinePlayer>> envOnlinePlayers = new ConcurrentHashMap<Environment, Map<String, OnlinePlayer>>();
    private Map<Environment, List<String>> envCharactersToCheck = new HashMap<Environment, List<String>>();
    
    private Query self = this;

    public PopulationManager()
    {
        Vertx vertx = eventTracker.getVertx();
        
        for(Environment environment : Environment.values())
        {
            envOnlinePlayers.put(environment, new ConcurrentHashMap<String, OnlinePlayer>());
            envCharactersToCheck.put(environment, new ArrayList<String>());
        }

        //Regularly sends events for population data.
        vertx.setPeriodic(10000, new Handler<Long>()
        {
            @Override
            public void handle(Long event)
            {
                generateEvents();
            }
        });

        //Detects if players are no-longer online (i.e. we missed the logout event.)
        vertx.setPeriodic(60000, new Handler<Long>()
        {
            @Override
            public void handle(Long timerID)
            {
                //Check players have not logged off on all environments.
                for(Entry<Environment, List<String>> charactersToCheckEntry : envCharactersToCheck.entrySet())
                {
                    Environment environment = charactersToCheckEntry.getKey();
                    List<String> charactersToCheck = charactersToCheckEntry.getValue();
                    
                    //Remove characters that census failed to retrieve online statuses for (deleted characters, low BR, etc), or has not seen an event in 15 mins
                    for (String characterID : charactersToCheck)
                    {
                        if (!dynamicDataManager.characterDataExists(characterID))
                        {
                            envOnlinePlayers.get(environment).remove(characterID);
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

                    List<String> characters = new ArrayList<String>();

                    for (String characterID : charactersToCheck)
                    {
                        characters.add(characterID);

                        if (characters.size() >= 150)
                        {
                            queryManager.getCensusData("character?character_id=" + StringUtils.join(characters, ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                                environment, true, self);

                            characters = new ArrayList<String>();
                        }
                    }

                    if (!characters.isEmpty())
                    {
                        queryManager.getCensusData("character?character_id=" + StringUtils.join(characters, ",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^show:outfit_id^inject_at:outfit,characters_online_status^on:character_id^to:character_id^inject_at:online,characters_world^on:character_id^to:character_id^inject_at:world,characters_event^on:character_id^to:character_id^terms:type=DEATH^inject_at:last_event",
                            environment, true, self);
                    }
                }
            }
        });
    }

    private void generateEvents()
    {
        //Generate events for all environments
        for(Entry<Environment, Map<String, OnlinePlayer>> onlinePlayersEntry : envOnlinePlayers.entrySet())
        {
            Environment environment = onlinePlayersEntry.getKey();
            Map<String, OnlinePlayer> onlinePlayers = onlinePlayersEntry.getValue();
            
            PopulationStore totalPopulation = new PopulationStore();
            Map<World, PopulationStore> worldPopulations = new HashMap<World, PopulationStore>();
            Map<String, PopulationStore> zonePopulations = new HashMap<String, PopulationStore>();

            for (World world : World.getAllWorlds())
            {
                worldPopulations.put(world, new PopulationStore());

                for (Zone zone : Zone.getAllZones())
                {
                    zonePopulations.put(world.getID() + "_" + zone.getID(), new PopulationStore());
                }
            }

            //Count Players
            for (OnlinePlayer player : onlinePlayers.values())
            {
                Faction faction = player.getFaction();
                String outfitID = player.getOutfitID();
                String worldID = player.getWorld().getID();
                String zoneID = player.getZone().getID();

                worldPopulations.get(player.getWorld()).incrementPopulation(faction, outfitID);
                zonePopulations.get(worldID + "_" + zoneID).incrementPopulation(faction, outfitID);

                totalPopulation.incrementPopulation(faction, outfitID);
            }

            //Post Events
            String eventName = "PopulationChange";

            //Total Population
            JsonObject totalPayload = new JsonObject();
            totalPayload.putString("population_type", "total");
            totalPayload.putString("population_total", totalPopulation.getTotalPopulation().toString());
            totalPayload.putString("population_vs", totalPopulation.getFactionPopulation(Faction.VS).toString());
            totalPayload.putString("population_nc", totalPopulation.getFactionPopulation(Faction.NC).toString());
            totalPayload.putString("population_tr", totalPopulation.getFactionPopulation(Faction.TR).toString());

            eventTracker.getEventHandler().handleEvent(eventName, totalPayload, environment);

            //World Populations
            for (World world : World.getValidWorlds())
            {
                JsonObject worldPayload = new JsonObject();
                PopulationStore worldPopulation = worldPopulations.get(world);

                worldPayload.putString("population_type", "world");
                worldPayload.putString("population_total", worldPopulation.getTotalPopulation().toString());
                worldPayload.putString("population_vs", worldPopulation.getFactionPopulation(Faction.VS).toString());
                worldPayload.putString("population_nc", worldPopulation.getFactionPopulation(Faction.NC).toString());
                worldPayload.putString("population_tr", worldPopulation.getFactionPopulation(Faction.TR).toString());
                worldPayload.putString("world_id", world.getID());

                eventTracker.getEventHandler().handleEvent(eventName, worldPayload, environment);

                for (Entry<String, Integer> outfit : worldPopulation.getOutfitPopulations().entrySet())
                {
                    JsonObject outfitPayload = new JsonObject();

                    outfitPayload.putString("population_type", "outfit");
                    outfitPayload.putString("population_total", outfit.getValue().toString());
                    outfitPayload.putString("outfit_id", outfit.getKey());
                    outfitPayload.putString("world_id", world.getID());

                    eventTracker.getEventHandler().handleEvent(eventName, outfitPayload, environment);
                }

                for (Zone zone : Zone.getValidZones())
                {
                    JsonObject zonePayload = new JsonObject();

                    PopulationStore zonePopulation = zonePopulations.get(world.getID() + "_" + zone.getID());

                    zonePayload.putString("population_type", "zone");
                    zonePayload.putString("population_total", zonePopulation.getTotalPopulation().toString());
                    zonePayload.putString("population_vs", zonePopulation.getFactionPopulation(Faction.VS).toString());
                    zonePayload.putString("population_nc", zonePopulation.getFactionPopulation(Faction.NC).toString());
                    zonePayload.putString("population_tr", zonePopulation.getFactionPopulation(Faction.TR).toString());
                    zonePayload.putString("zone_id", zone.getID());
                    zonePayload.putString("world_id", world.getID());

                    eventTracker.getEventHandler().handleEvent(eventName, zonePayload, environment);

                    for (Entry<String, Integer> outfit : worldPopulation.getOutfitPopulations().entrySet())
                    {
                        JsonObject outfitPayload = new JsonObject();

                        outfitPayload.putString("population_type", "zone_outfit");
                        outfitPayload.putString("population_total", outfit.getValue().toString());
                        outfitPayload.putString("outfit_id", outfit.getKey());
                        outfitPayload.putString("zone_id", zone.getID());
                        outfitPayload.putString("world_id", world.getID());

                        eventTracker.getEventHandler().handleEvent(eventName, outfitPayload, environment);
                    }
                }
            }
        }
    }

    @Override
    public void receiveData(JsonObject data, Environment environment)
    {
        JsonArray characterList = data.getArray("character_list");

        for (int i = 0; i < characterList.size(); i++)
        {
            //Update Character Info
            JsonObject characterData = characterList.get(i);

            String characterID = characterData.getString("character_id");
            String characterName = characterData.getObject("name").getString("first");
            String factionID = characterData.getString("faction_id");

            String outfitID;
            String zoneID;
            String worldID;
            Boolean online;

            if (characterData.containsField("outfit"))
            {
                outfitID = characterData.getObject("outfit").getString("outfit_id");
            }
            else
            {
                outfitID = "0";
            }

            if (characterData.containsField("last_event"))
            {
                zoneID = characterData.getObject("last_event").getString("zone_id");
            }
            else
            {
                zoneID = "0";
            }

            if (characterData.containsField("world"))
            {
                worldID = characterData.getObject("world").getString("world_id");
            }
            else
            {
                worldID = "0";
            }

            if (characterData.containsField("online"))
            {
                online = !characterData.getObject("online").getString("online_status").equals("0");
            }
            else
            {
                online = false;
            }

            CharacterInfo character = new CharacterInfo(characterID, characterName, factionID, outfitID, zoneID, worldID, online);

            dynamicDataManager.addCharacterData(characterID, character);

            //Verify Population Data.
            if (dynamicDataManager.characterDataExists(characterID))
            {
                CharacterInfo characterInfo = dynamicDataManager.getCharacterData(characterID);

                if (characterInfo.isOnline())
                {
                    envOnlinePlayers.get(environment).get(characterID).setLastEvent(new Date());
                }

                else
                {
                    //This player doesn't have valid online status data.
                    //This player is no-longer online. Remove the player.
                    envOnlinePlayers.get(environment).remove(characterID);
                }
            }
        }

        generateEvents();
    }
}
