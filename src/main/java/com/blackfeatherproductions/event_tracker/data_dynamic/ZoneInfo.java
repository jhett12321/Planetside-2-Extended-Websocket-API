package com.blackfeatherproductions.event_tracker.data_dynamic;

import java.util.HashMap;
import java.util.Map;

import com.blackfeatherproductions.event_tracker.data_static.Facility;
import com.blackfeatherproductions.event_tracker.data_static.Faction;

public class ZoneInfo
{
    private Boolean locked;
    private Faction locking_faction;

    private Map<Facility, FacilityInfo> facilities = new HashMap<Facility, FacilityInfo>();

    /**
     * @return the locking faction of this zone, otherwise null if the zone is
     * not locked.
     */
    public Faction getLockingFaction()
    {
        return locking_faction;
    }

    public void addFacility(Facility facility, FacilityInfo facilityInfo)
    {

    }

    /**
     * @param locking_faction the locking faction of this zone, otherwise null
     * if the zone is not locked.
     */
    public void setLockingFaction(Faction locking_faction)
    {
        this.locking_faction = locking_faction;
    }

    /**
     * @return locked true if the zone is locked, false if the zone is not
     * locked.
     */
    public Boolean isLocked()
    {
        return locked;
    }

    /**
     * @param locked true if the zone is locked, false if the zone is not
     * locked.
     */
    public void setLocked(Boolean locked)
    {
        this.locked = locked;
    }

    /**
     * @return the FacilityInfo object representing the facility_id, otherwise
     * null if it does not exist.
     */
    public FacilityInfo getFacility(Facility facility)
    {
        if (facility != null)
        {
            if (!facilities.containsKey(facility))
            {
                facilities.put(facility, new FacilityInfo());
            }

            return facilities.get(facility);
        }

        return null;
    }

    public Map<Facility, FacilityInfo> getFacilities()
    {
        return facilities;
    }
}
