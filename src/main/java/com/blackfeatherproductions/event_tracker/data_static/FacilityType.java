package com.blackfeatherproductions.event_tracker.data_static;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FacilityType
{
    public static Map<String, FacilityType> facilityTypes = new HashMap<String, FacilityType>();

    public static FacilityType NONE;
    public static FacilityType DEFAULT;
    public static FacilityType AMP_STATION;
    public static FacilityType BIO_LAB;
    public static FacilityType TECH_PLANT;
    public static FacilityType LARGE_OUTPOST;
    public static FacilityType SMALL_OUTPOST;
    public static FacilityType WARPGATE;
    public static FacilityType INTERLINK_FACILITY;

    private final String id;
    private final String desc;

    public FacilityType(String id, String desc)
    {
        this.id = id;
        this.desc = desc;
    }

    public String getID()
    {
        return id;
    }

    public String getDesc()
    {
        return desc;
    }

    public static FacilityType getFacilityTypeByID(String id)
    {
        if(facilityTypes.containsKey(id))
        {
            return facilityTypes.get(id);
        }
        else
        {
            return FacilityType.DEFAULT;
        }
    }

    public static FacilityType getFacilityTypeByName(String name)
    {
        for (FacilityType facilityType : facilityTypes.values())
        {
            if (facilityType.getDesc().equalsIgnoreCase(name))
            {
                return facilityType;
            }
        }
        
        return FacilityType.DEFAULT;
    }

    public static Collection<FacilityType> getAllFacilityTypes()
    {
        return facilityTypes.values();
    }

    public static Collection<FacilityType> getValidFacilityTypes()
    {
        Collection<FacilityType> validTypes = new ArrayList<FacilityType>();

        for (FacilityType facilityType : facilityTypes.values())
        {
            if (facilityType != FacilityType.DEFAULT)
            {
                validTypes.add(facilityType);
            }
        }

        return validTypes;
    }
}
