package com.blackfeatherproductions.event_tracker.events.extended.recursion;

import com.blackfeatherproductions.event_tracker.data_dynamic.OnlinePlayer;

public class RecursionPlayer
{
    private OnlinePlayer player;
    
    private int groupKillAmnt = 0;
    private int killStreak = 0;
    private int headShotStreak = 0;
    
    public RecursionPlayer(OnlinePlayer player)
    {
        this.player = player;
    }
}
