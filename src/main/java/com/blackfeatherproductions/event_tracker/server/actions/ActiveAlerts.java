package com.blackfeatherproductions.event_tracker.server.actions;

import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import com.blackfeatherproductions.event_tracker.Utils;

@ActionInfo(actionNames = "activeAlerts")
public class ActiveAlerts implements Action
{
	@Override
	public void processAction(ServerWebSocket clientConnection, JsonObject actionData)
	{
/*		JsonObject response = new JsonObject();
		
		JsonObject alerts;
		
		if(actionData.containsField("worlds"))
		{
			alerts = Utils.getActiveAlerts(actionData.getArray("worlds"));
		}
		else
		{
			alerts = Utils.getActiveAlerts(null);
		}
		
		//Send Client Response
		response.putString("action", "ActiveAlerts");
		response.putObject("alerts", alerts);
		
		clientConnection.writeTextFrame(response.encode());*/
	}

}
