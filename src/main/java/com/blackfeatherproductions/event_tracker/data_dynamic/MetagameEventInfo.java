package com.blackfeatherproductions.event_tracker.data_dynamic;

import com.blackfeatherproductions.event_tracker.data_static.MetagameEventType;

public class MetagameEventInfo
{
    private final MetagameEventType type;
    private final String startTime;
    private final String endTime;
    private final String instanceID;

    public MetagameEventInfo(String instanceID, MetagameEventType type, String startTime, String endTime)
    {
        this.instanceID = instanceID;
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
