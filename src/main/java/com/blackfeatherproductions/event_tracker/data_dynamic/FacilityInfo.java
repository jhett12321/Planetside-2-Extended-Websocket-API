package com.blackfeatherproductions.event_tracker.data_dynamic;

import com.blackfeatherproductions.event_tracker.data_static.Faction;

public class FacilityInfo
{
    private Faction owner;
    
    private boolean blocked = false;

    public Faction getOwner()
    {
        return owner;
    }

    public void setOwner(Faction owner)
    {
        this.owner = owner;
    }

    public boolean isBlocked()
    {
        return blocked;
    }

    public void setBlocked(boolean blocked)
    {
        this.blocked = blocked;
    }
}
