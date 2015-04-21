package com.blackfeatherproductions.event_tracker.events.extended.population;

import java.util.HashMap;
import java.util.Map;

import com.blackfeatherproductions.event_tracker.data_static.Faction;

public class PopulationStore
{
    private Integer totalPopulation = 0;

    Map<Faction, Integer> factionPopulations = new HashMap<Faction, Integer>();
    Map<String, Integer> outfitPopulations = new HashMap<String, Integer>();

    public PopulationStore()
    {
        factionPopulations.put(Faction.VS, 0);
        factionPopulations.put(Faction.NC, 0);
        factionPopulations.put(Faction.TR, 0);
    }

    public void incrementPopulation(Faction faction, String outfit)
    {
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
