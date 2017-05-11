package com.blackfeatherproductions.event_tracker.server.actions;

import java.util.Map.Entry;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.ZoneInfo;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;
import com.blackfeatherproductions.event_tracker.utils.CensusUtils;
import com.blackfeatherproductions.event_tracker.utils.TerritoryInfo;
import com.blackfeatherproductions.event_tracker.utils.TerritoryUtils;

@ActionInfo(actionNames = "zoneStatus")
public class ZoneStatus implements Action
{
    private final DynamicDataManager dynamicDataManager = EventTracker.instance.getDynamicDataManager();

    @Override
    public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
    {
        JsonObject response = new JsonObject();
        response.put("action", "zoneStatus");

        JsonObject worlds = new JsonObject();

        if (actionData.containsKey("worlds"))
        {
            for (int i = 0; i < actionData.getJsonArray("worlds").size(); i++)
            {
                if (CensusUtils.isValidWorld(actionData.getJsonArray("worlds").getString(i)))
                {
                    World world = World.getWorldByID(actionData.getJsonArray("worlds").getString(i));

                    JsonObject worldObj = new JsonObject();
                    JsonObject zones = new JsonObject();

                    for (Entry<Zone, ZoneInfo> zoneInfo : dynamicDataManager.getWorldInfo(world).getZones().entrySet())
                    {
                        JsonObject zone = new JsonObject();

                        String locked = "0";
                        if (zoneInfo.getValue().isLocked())
                        {
                            locked = "1";
                        }

                        zone.put("locked", locked);
                        zone.put("locked_by", zoneInfo.getValue().getLockingFaction().getID());

                        TerritoryInfo controlInfo = TerritoryUtils.calculateTerritoryControl(world, zoneInfo.getKey());

                        zone.put("control_vs", String.valueOf(controlInfo.controlVS));
                        zone.put("control_nc", String.valueOf(controlInfo.controlNC));
                        zone.put("control_tr", String.valueOf(controlInfo.controlTR));

                        zones.put(zoneInfo.getKey().getID(), zone);
                    }

                    worldObj.put("zones", zones);
                    worlds.put(world.getID(), worldObj);
                }
            }
        }
        else
        {
            for (Entry<World, WorldInfo> world : dynamicDataManager.getAllWorldInfo().entrySet())
            {
                JsonObject worldObj = new JsonObject();
                JsonObject zones = new JsonObject();

                for (Entry<Zone, ZoneInfo> zoneInfo : world.getValue().getZones().entrySet())
                {
                    JsonObject zone = new JsonObject();

                    String locked = "0";
                    if (zoneInfo.getValue().isLocked())
                    {
                        locked = "1";
                    }

                    zone.put("locked", locked);
                    zone.put("locked_by", zoneInfo.getValue().getLockingFaction().getID());

                    TerritoryInfo controlInfo = TerritoryUtils.calculateTerritoryControl(world.getKey(), zoneInfo.getKey());

                    zone.put("control_vs", String.valueOf(controlInfo.controlVS));
                    zone.put("control_nc", String.valueOf(controlInfo.controlNC));
                    zone.put("control_tr", String.valueOf(controlInfo.controlTR));

                    zones.put(zoneInfo.getKey().getID(), zone);
                }

                worldObj.put("zones", zones);
                worlds.put(world.getKey().getID(), worldObj);
            }
        }

        //Send Client Response
        response.put("worlds", worlds);

        clientConnection.writeFinalTextFrame(response.encode());
    }
}
