package com.blackfeatherproductions.event_tracker;

import java.util.ArrayList;
import java.util.List;

import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.data_static.FacilityType;
import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.MetagameEventType;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class StaticDataManager
{
    protected static void Init()
    {
        //Initialises Static Game Data

        //TODO FUTURE Query Census for all required static data
        //TODO FUTURE All Data Types need to eventually have their own class.
        //TODO FUTURE Types to be implemented: achievement, item/weapon, vehicle, directive
        
        //Environments
        Environment.PC = new Environment("PC", "pc", "ps2:v2", "ps2");
        Environment.PS4_US = new Environment("PS4_US", "ps4_us", "ps2ps4us:v2", "ps2ps4us");
        Environment.PS4_EU = new Environment("PS4_EU", "ps4_eu", "ps2ps4eu:v2", "ps2ps4eu");
        Environment.WEBSOCKET_SERVICE = new Environment("WEBSOCKET_SERVICE", "websocket_service", null, null);
        
        Environment.environments.add(Environment.PC);
        Environment.environments.add(Environment.PS4_US);
        Environment.environments.add(Environment.PS4_EU);
        Environment.environments.add(Environment.WEBSOCKET_SERVICE);
        
        //Worlds
        World.UNKNOWN = new World("0", "Unknown");
        World.worlds.put("0", World.UNKNOWN);

        //PC
        World.worlds.put("1", new World("1", "Connery"));
        World.worlds.put("10", new World("10", "Miller"));
        World.worlds.put("13", new World("13", "Cobalt"));
        World.worlds.put("17", new World("17", "Emerald"));
        World.worlds.put("19", new World("19", "Jaeger"));
        World.worlds.put("25", new World("25", "Briggs"));

        //PS4US
        World.worlds.put("1000", new World("1000", "Genudine"));
        World.worlds.put("1001", new World("1001", "Palos"));
        World.worlds.put("1002", new World("1002", "Crux"));

        //PS4EU
        World.worlds.put("2000", new World("2000", "Ceres"));
        World.worlds.put("2001", new World("2001", "Lithcorp"));

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
        
        //Facility Types
        FacilityType.NONE = new FacilityType("0", "INTERNAL: No FacilityType");
        FacilityType.DEFAULT = new FacilityType("1", "Default");
        FacilityType.AMP_STATION = new FacilityType("2", "Amp Station");
        FacilityType.BIO_LAB = new FacilityType("3", "Bio Lab");
        FacilityType.TECH_PLANT = new FacilityType("4", "Tech Plant");
        FacilityType.LARGE_OUTPOST = new FacilityType("5", "Large Outpost");
        FacilityType.SMALL_OUTPOST = new FacilityType("6", "Small Outpost");
        FacilityType.WARPGATE = new FacilityType("7", "Warpgate");
        FacilityType.INTERLINK_FACILITY = new FacilityType("8", "Interlink Facility");
        
        FacilityType.facilityTypes.put("0", FacilityType.NONE);
        FacilityType.facilityTypes.put("1", FacilityType.DEFAULT);
        FacilityType.facilityTypes.put("2", FacilityType.AMP_STATION);
        FacilityType.facilityTypes.put("3", FacilityType.BIO_LAB);
        FacilityType.facilityTypes.put("4", FacilityType.TECH_PLANT);
        FacilityType.facilityTypes.put("5", FacilityType.LARGE_OUTPOST);
        FacilityType.facilityTypes.put("6", FacilityType.SMALL_OUTPOST);
        FacilityType.facilityTypes.put("7", FacilityType.WARPGATE);
        FacilityType.facilityTypes.put("8", FacilityType.INTERLINK_FACILITY);

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
    }
    
    protected static void CensusInit()
    {
        //Facilities (CENSUS)
        EventTracker.instance.getQueryManager().queryFacilityStaticData();
        //Metagame Events
        EventTracker.instance.getQueryManager().queryMetagameEventStaticData();
    }
}
