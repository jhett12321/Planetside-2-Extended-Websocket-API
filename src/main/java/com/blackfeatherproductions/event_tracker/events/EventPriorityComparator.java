package com.blackfeatherproductions.event_tracker.events;

import java.util.Comparator;

import com.blackfeatherproductions.event_tracker.QueuedEvent;

public class EventPriorityComparator implements Comparator<QueuedEvent>
{
    @Override
    public int compare(QueuedEvent o1, QueuedEvent o2)
    {
        int result = o1.getPriority().compareTo(o2.getPriority());
        
        if(result == 0)
        {
            if(o1.getEventTimestamp() != null && o2.getEventTimestamp() != null)
            {
                result = o1.getEventTimestamp().compareTo(o2.getEventTimestamp());
            }
            
            if(result == 0)
            {
                result = o1.getCreationTimestamp().compareTo(o2.getCreationTimestamp());
            }
        }
        
        return result;
    }

}
