package com.blackfeatherproductions.event_tracker.events;

import java.util.Comparator;

import com.blackfeatherproductions.event_tracker.QueuedEvent;

public class EventPriorityComparator implements Comparator<QueuedEvent>
{
    @Override
    public int compare(QueuedEvent o1, QueuedEvent o2)
    {
        return o1.getPriority().compareTo(o2.getPriority());
    }

}
