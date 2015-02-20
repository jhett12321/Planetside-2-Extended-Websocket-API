package com.blackfeatherproductions.event_tracker.server.actions;

import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.Utils;

@ActionInfo(actionNames = "zoneStatus")
public class ZoneStatus implements Action
{
	@Override
	public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
	{
		//TODO
/*		JsonObject response = new JsonObject();
		JsonObject zones;
		
		if(actionData.containsField("worlds"))
		{
			zones = Utils.getZoneStatus(actionData.getArray("worlds"));
		}
		
		else
		{
			zones = Utils.getZoneStatus(null);
		}
		
		response.putObject("zones", zones);*/
	}
}
