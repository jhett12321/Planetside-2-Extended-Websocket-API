package com.blackfeatherproductions.event_tracker;

import java.util.ArrayList;
import java.util.List;

import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.MetagameEventType;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class StaticDataManager
{
    protected StaticDataManager()
    {
        //Initialises Static Game Data

        //TODO 1.1 Query Census for all required static data
        //TODO 1.1 All Data Types need to eventually have their own class.
        //TODO 1.1 Types to be implemented: achievement, item/weapon, vehicle, directive
        //Worlds
        World.UNKNOWN = new World("0", "Unknown");
        World.CONNERY = new World("1", "Connery");
        World.MILLER = new World("10", "Miller");
        World.COBALT = new World("13", "Cobalt");
        World.EMERALD = new World("17", "Emerald");
        World.JAEGAR = new World("19", "Jaeger");
        World.BRIGGS = new World("25", "Briggs");

        World.worlds.put("0", World.UNKNOWN);
        World.worlds.put("1", World.CONNERY);
        World.worlds.put("10", World.MILLER);
        World.worlds.put("13", World.COBALT);
        World.worlds.put("17", World.EMERALD);
        World.worlds.put("19", World.JAEGAR);
        World.worlds.put("25", World.BRIGGS);

        //Zones
        Zone.UNKNOWN = new Zone("0", "Unknown", "INTERNAL: This is an unknown zone.");
        Zone.INDAR = new Zone("2", "Indar", "The arid continent of Indar is home to an assortment of biomes. Grassy savannas, rocky canyons, and the open plains of the seabed provide unique challenges to soldiers.");
        Zone.HOSSIN = new Zone("4", "Hossin", "Hossin's dense mangrove and willow forests provide air cover along its many swamps and highlands.");
        Zone.AMERISH = new Zone("6", "Amerish", "Amerish's lush groves and rocky outcroppings provide ample cover between its rolling plains and mountain passes.");
        Zone.ESAMIR = new Zone("8", "Esamir", "Esamir's expanses of frigid tundra and craggy mountains provide little cover from airborne threats.");

        Zone.zones.put("0", Zone.UNKNOWN);
        Zone.zones.put("2", Zone.INDAR);
        Zone.zones.put("4", Zone.HOSSIN);
        Zone.zones.put("6", Zone.AMERISH);
        Zone.zones.put("8", Zone.ESAMIR);

        //Factions
        List<String> loadoutsNS = new ArrayList<>();
        loadoutsNS.add("0");

        List<String> loadoutsVS = new ArrayList<>();
        loadoutsVS.add("15");
        loadoutsVS.add("17");
        loadoutsVS.add("18");
        loadoutsVS.add("19");
        loadoutsVS.add("20");
        loadoutsVS.add("21");

        List<String> loadoutsNC = new ArrayList<>();
        loadoutsNC.add("1");
        loadoutsNC.add("3");
        loadoutsNC.add("4");
        loadoutsNC.add("5");
        loadoutsNC.add("6");
        loadoutsNC.add("7");

        List<String> loadoutsTR = new ArrayList<>();
        loadoutsTR.add("8");
        loadoutsTR.add("10");
        loadoutsTR.add("11");
        loadoutsTR.add("12");
        loadoutsTR.add("13");
        loadoutsTR.add("14");

        Faction.NS = new Faction("0", loadoutsNS, "Nanite Systems", "NS");
        Faction.VS = new Faction("1", loadoutsVS, "Vanu Sovereignty", "VS");
        Faction.NC = new Faction("2", loadoutsNC, "New Conglomerate", "NC");
        Faction.TR = new Faction("3", loadoutsTR, "Terran Republic", "TR");

        Faction.factions.put("0", Faction.NS);
        Faction.factions.put("1", Faction.VS);
        Faction.factions.put("2", Faction.NC);
        Faction.factions.put("3", Faction.TR);

        //Metagame Event Types
        MetagameEventType.metagameEventTypes.put("1", new MetagameEventType("1", "Feeling the Heat", "Capture Indar within the time limit", Zone.INDAR, "0", "1"));
        MetagameEventType.metagameEventTypes.put("2", new MetagameEventType("2", "Cold War", "Capture Esamir within the time limit", Zone.ESAMIR, "0", "1"));
        MetagameEventType.metagameEventTypes.put("3", new MetagameEventType("3", "Seeing Green", "Capture Amerish within the time limit", Zone.AMERISH, "0", "1"));
        MetagameEventType.metagameEventTypes.put("4", new MetagameEventType("4", "Marsh Madness", "Capture Hossin within the time limit", Zone.HOSSIN, "0", "1"));
        MetagameEventType.metagameEventTypes.put("51", new MetagameEventType("51", "Indar Pumpkin Hunt", "Seek and destroy pumpkins on Indar", Zone.INDAR, "0", "5"));
        MetagameEventType.metagameEventTypes.put("52", new MetagameEventType("52", "Esamir Pumpkin Hunt", "Seek and destroy pumpkins on Esamir", Zone.ESAMIR, "0", "5"));
        MetagameEventType.metagameEventTypes.put("53", new MetagameEventType("53", "Amerish Pumpkin Hunt", "Seek and destroy pumpkins on Amerish", Zone.AMERISH, "0", "5"));
        MetagameEventType.metagameEventTypes.put("54", new MetagameEventType("54", "Hossin Pumpkin Hunt", "Seek and destroy pumpkins on Hossin", Zone.HOSSIN, "0", "5"));
    }
}
