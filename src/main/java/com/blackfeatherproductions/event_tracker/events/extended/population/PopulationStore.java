package com.blackfeatherproductions.event_tracker.events.extended.population;

import com.blackfeatherproductions.event_tracker.data_dynamic.OnlinePlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_static.Faction;

public class PopulationStore
{
    private Integer totalPopulation = 0;

    Map<Faction, Integer> factionPopulations = new ConcurrentHashMap<Faction, Integer>();
    Map<String, Integer> outfitPopulations = new ConcurrentHashMap<String, Integer>();

    protected PopulationStore()
    {
        factionPopulations.put(Faction.VS, 0);
        factionPopulations.put(Faction.NC, 0);
        factionPopulations.put(Faction.TR, 0);
    }

    protected void incrementPopulation(OnlinePlayer player)
    {
        if (player != null)
        {
            Faction faction = player.getFaction();
            String outfit = player.getOutfitID();

            if (factionPopulations.containsKey(faction))
            {
                factionPopulations.put(faction, factionPopulations.get(faction) + 1);
                totalPopulation++;

                if (!outfitPopulations.containsKey(outfit))
                {
                    outfitPopulations.put(outfit, 0);
                }

                outfitPopulations.put(outfit, outfitPopulations.get(outfit) + 1);
            }
        }
    }
    
    protected void decrementPopulation(OnlinePlayer player)
    {
        if (player != null)
        {
            Faction faction = player.getFaction();
            String outfit = player.getOutfitID();

            if (factionPopulations.containsKey(faction))
            {
                factionPopulations.put(faction, factionPopulations.get(faction) - 1);
                totalPopulation--;

                if (!outfitPopulations.containsKey(outfit))
                {
                    outfitPopulations.put(outfit, 0);
                }

                outfitPopulations.put(outfit, outfitPopulations.get(outfit) - 1);
            }
        }
    }

    public Integer getTotalPopulation()
    {
        return totalPopulation;
    }

    public Integer getFactionPopulation(Faction faction)
    {
        return factionPopulations.get(faction);
    }

    public Integer getOutfitPopulation(String outfit)
    {
        return outfitPopulations.get(outfit);
    }

    public Map<String, Integer> getOutfitPopulations()
    {
        return outfitPopulations;
    }
}
