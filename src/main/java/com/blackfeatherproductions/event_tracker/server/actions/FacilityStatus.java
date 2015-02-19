package com.blackfeatherproductions.event_tracker.server.actions;

import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.Utils;

@ActionInfo(actionNames = "facilityStatus")
public class FacilityStatus implements Action
{
	@Override
	public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
	{
/*		if(actionData.containsField("worlds") && actionData.getArray("worlds").size() > 0 && actionData.containsField("zones") && actionData.getArray("zones").size() > 0)
		{
			JsonObject facilityStatus = new JsonObject();
			
			for(int i=0; i<actionData.getArray("worlds").size(); i++)
			{
				String worldID = actionData.getArray("worlds").get(i);

				if(Utils.isValidWorld(worldID))
				{
					JsonObject world = new JsonObject();
					
					
					facilityStatus.putObject(worldID, world);
				}
			}
		}*/
	}
}
