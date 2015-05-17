package com.blackfeatherproductions.event_tracker.server.actions;

import java.util.Map.Entry;

import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.DynamicDataManager;
import com.blackfeatherproductions.event_tracker.EventTracker;
import com.blackfeatherproductions.event_tracker.Utils;
import com.blackfeatherproductions.event_tracker.data_dynamic.WorldInfo;
import com.blackfeatherproductions.event_tracker.data_dynamic.ZoneInfo;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

@ActionInfo(actionNames = "zoneStatus")
public class ZoneStatus implements Action
{
    private final DynamicDataManager dynamicDataManager = EventTracker.getInstance().getDynamicDataManager();

    @Override
    public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
    {
        JsonObject response = new JsonObject();
        response.putString("action", "zoneStatus");

        JsonObject worlds = new JsonObject();

        if (actionData.containsField("worlds"))
        {
            for (int i = 0; i < actionData.getArray("worlds").size(); i++)
            {
                if (Utils.isValidWorld((String) actionData.getArray("worlds").get(i)))
                {
                    World world = World.getWorldByID((String) actionData.getArray("worlds").get(i));

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

                        zone.putString("locked", locked);
                        zone.putString("locked_by", zoneInfo.getValue().getLockingFaction().getID());

                        JsonObject controlInfo = Utils.calculateTerritoryControl(world, zoneInfo.getKey());

                        zone.putString("control_vs", controlInfo.getString("control_vs"));
                        zone.putString("control_nc", controlInfo.getString("control_nc"));
                        zone.putString("control_tr", controlInfo.getString("control_tr"));

                        zones.putObject(zoneInfo.getKey().getID(), zone);
                    }

                    worldObj.putObject("zones", zones);
                    worlds.putObject(world.getID(), worldObj);
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

                    zone.putString("locked", locked);
                    zone.putString("locked_by", zoneInfo.getValue().getLockingFaction().getID());

                    JsonObject controlInfo = Utils.calculateTerritoryControl(world.getKey(), zoneInfo.getKey());

                    zone.putString("control_vs", controlInfo.getString("control_vs"));
                    zone.putString("control_nc", controlInfo.getString("control_nc"));
                    zone.putString("control_tr", controlInfo.getString("control_tr"));

                    zones.putObject(zoneInfo.getKey().getID(), zone);
                }

                worldObj.putObject("zones", zones);
                worlds.putObject(world.getKey().getID(), worldObj);
            }
        }

        //Send Client Response
        response.putObject("worlds", worlds);

        clientConnection.writeTextFrame(response.encode());
    }
}
