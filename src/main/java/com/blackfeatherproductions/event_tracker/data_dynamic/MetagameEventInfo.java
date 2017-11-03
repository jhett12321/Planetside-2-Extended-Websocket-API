package com.blackfeatherproductions.event_tracker.data_dynamic;

import com.blackfeatherproductions.event_tracker.data_static.MetagameEventType;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class MetagameEventInfo
{
    private final MetagameEventType type;
    private final String startTime;
    private final String endTime;
    private final String instanceID;
    private final Zone zone;

    public MetagameEventInfo(String instanceID, Zone zone, MetagameEventType type, String startTime, String endTime)
    {
        this.instanceID = instanceID;
        this.zone = zone;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * @return the instanceID
     */
    public String getInstanceID()
    {
        return instanceID;
    }

    public Zone getZone()
    {
        return zone;
    }

    /**
     * @return the type
     */
    public MetagameEventType getType()
    {
        return type;
    }

    /**
     * @return the startTime
     */
    public String getStartTime()
    {
        return startTime;
    }

    /**
     * @return the endTime
     */
    public String getEndTime()
    {
        return endTime;
    }
}
