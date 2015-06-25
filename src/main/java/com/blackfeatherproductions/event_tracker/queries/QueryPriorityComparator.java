package com.blackfeatherproductions.event_tracker.queries;

import java.util.Comparator;

public class QueryPriorityComparator implements Comparator<CensusQuery>
{
    @Override
    public int compare(CensusQuery o1, CensusQuery o2)
    {
        return o1.getPriority().compareTo(o2.getPriority());
    }

}