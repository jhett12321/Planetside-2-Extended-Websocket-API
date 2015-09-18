package com.blackfeatherproductions.event_tracker.utils;

import com.blackfeatherproductions.event_tracker.data_static.Faction;

public class TerritoryInfo
{
    public final int controlVS;
    public final int controlNC;
    public final int controlTR;

    public final Faction majorityController;

    public final int totalVS;
    public final int totalNC;
    public final int totalTR;

    public TerritoryInfo(int controlVS, int controlNC, int controlTR, Faction majorityController, int totalVS, int totalNC, int totalTR)
    {
        this.controlVS = controlVS;
        this.controlNC = controlNC;
        this.controlTR = controlTR;

        this.majorityController = majorityController;

        this.totalVS = totalVS;
        this.totalNC = totalNC;
        this.totalTR = totalTR;
    }
}