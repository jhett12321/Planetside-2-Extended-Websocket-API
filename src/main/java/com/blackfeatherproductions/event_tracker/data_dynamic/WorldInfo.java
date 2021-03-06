package com.blackfeatherproductions.event_tracker.data_dynamic;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.data_static.Environment;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

//This is the main class for world-centric data.
//The DynamicDataManager allows the system to get the world instance.
public class WorldInfo
{
    private final World world;

    private final Map<Zone, ZoneInfo> zones = new HashMap<Zone, ZoneInfo>();
    private final Map<String, MetagameEventInfo> metagameEvents = new HashMap<String, MetagameEventInfo>();

    private boolean online = false;

    public WorldInfo(World world)
    {
        this.world = world;
    }

    public ZoneInfo getZoneInfo(Zone zone)
    {
        if (zone != null)
        {
            if (!zones.containsKey(zone))
            {
                zones.put(zone, new ZoneInfo());
            }

            return zones.get(zone);
        }

        return null;
    }

    public World getWorld()
    {
        return world;
    }

    public Map<Zone, ZoneInfo> getZones()
    {
        return zones;
    }

    public Map<String, MetagameEventInfo> getActiveMetagameEvents()
    {
        return metagameEvents;
    }

    public MetagameEventInfo getActiveMetagameEvent(String instance_id)
    {
        return metagameEvents.get(instance_id);
    }

    public void addMetagameEvent(String instanceID, MetagameEventInfo metagameEventInfo)
    {
        metagameEvents.put(instanceID, metagameEventInfo);
    }

    public void removeMetagameEvent(String instanceID)
    {
        metagameEvents.remove(instanceID);
    }

    public boolean isOnline()
    {
        return online;
    }

    public void setOnline(boolean online)
    {
        if (online != this.online)
        {
            this.online = online;

            if (!online)
            {
                EventTracker.instance.getLogger().warn("Received Census Server State Message. " + world.getName() + " (" + world.getID() + ") is now OFFLINE.");
            }
            else
            {
                EventTracker.instance.getLogger().info("Received Census Server State Message. " + world.getName() + " (" + world.getID() + ") is now Online.");
            }

            //Generate Events
            JsonObject payload = new JsonObject();
            payload.put("online", online ? "1" : "0");
            payload.put("world_id", world.getID());

            EventTracker.instance.getEventHandler().handleEvent("ServiceStateChange", payload, Environment.WEBSOCKET_SERVICE);
        }
    }
}
