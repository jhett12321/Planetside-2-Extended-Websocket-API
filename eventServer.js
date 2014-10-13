/*
    This file is a part of the Planetside 2 Event Tracker program.
	The Event Tracker receives, filters and relays all event data sent by the SOE Census API. <http://census.soe.com/>
	
    Copyright (C) 2014  Jhett Black

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

//Includes
var WebSocketServer = require('ws').Server;
var http = require('http');
var url = require('url');
var mysql = require('mysql');

var eventTracker = require('./eventTracker.js');
var config = require('./config.js');

//Version
var version = "0.9.8";

//SOE Census Service ID
var serviceID = config.soeServiceID;

var pool = mysql.createPool(
{
	connectionLimit : config.apiDbConnectionLimit,
	host: config.apiDbHost,
	user: config.apiDbUser,
	password: config.apiDbPassword,
	database: config.apiDbName,
	supportBigNumbers: true,
	bigNumberStrings: true
});

//-------------------------------------------------------------------
/*
* 	WEBSOCKET SERVER
* 	Provides a live API to the events feed.
*/
//-------------------------------------------------------------------

/**********************
    Initialisation    *
**********************/

//HTTP server. Used for websocket server.
var httpServer = http.createServer(function(request, response)
{
	response.writeHead(404);
	response.end();
});

var clientConnections = {}; //Stores all connections to this server, and their subscribed events.
var alerts = {}; //Stores all active alerts
var connectionIDCounter = 0; //Connection Unique ID's.

httpServer.listen(config.serverPort, function()
{
	console.log((new Date()) + ' Websocket Server is listening on port ' + config.serverPort);
});

var wsServer = new WebSocketServer(
{
	server: httpServer,
	clientTracking: false, //We do our own tracking.
	verifyClient: function(info, callback)
	{
		try
		{
			var apiKey = url.parse(info.req.url, true).query.apikey;
			
			verifyAPIKey(apiKey, function(isValid, apiUsername) //Verify this client's API Key.
			{
				if(!isValid)
				{
					callback(false, 401, 'Invalid or Disabled API Key Specified.'); // Make sure we only accept requests from a valid API Key
				}
				else
				{
					callback(true);
				}
			});
		}
		catch(err)
		{
			callback(false, 401, 'Invalid API Key Specified');
		}
	}
});
	
/**************
    Events    *
**************/
	
wsServer.on('connection', function(clientConnection)
{
	var apiKey = url.parse(clientConnection.upgradeReq.url, true).query.apikey;
	
	verifyAPIKey(apiKey, function(isValid, apiUsername) //Verify this client's API Key.
	{
		if(isValid)
		{
			// Store a reference to the connection using an incrementing ID
			clientConnection.id = connectionIDCounter ++;
			
			// Store references relating to this connection's subscriptions
			clientConnection.subscriptions = getBlankSubscription();
			
			//Add to tracked client connections.
			clientConnections[clientConnection.id] = clientConnection;
		
			console.log((new Date()) + ' User ' + apiUsername + ' connected. API Key: ' + apiKey);
			
			var message = 
			{
				service: "ps2_events",
				version: version,
				websocket_event: "connectionStateChange",
				online: "true",
			}
			clientConnection.send(JSON.stringify(message));
			
			//Events
			clientConnection.on('message', function(message)
			{
				var decodedMessage;
				
				try
				{
					decodedMessage = JSON.parse(message);
				}
				catch(exception)
				{
					clientConnection.send('{"error": "BADJSON", "message": "You have supplied an invalid JSON string. Please check your syntax."}');
					decodedMessage = undefined;
				}
				
				if(decodedMessage != undefined)
				{
					var eventType = decodedMessage.event;
					var action = decodedMessage.action;
					
					if(action == "activeAlerts")
					{
						var alerts;
						if(decodedMessage.worlds != undefined)
						{
							alerts = eventTracker.getActiveAlerts(decodedMessage.worlds);
						}
						else
						{
							alerts = eventTracker.getActiveAlerts(null);
						}
						
						clientConnection.send(JSON.stringify(alerts));
					}
					
					else if(action == "zoneStatus")
					{
						var zones;
						if(decodedMessage.worlds != undefined)
						{
							zones = eventTracker.getZoneStatus(decodedMessage.worlds);
						}
						else
						{
							zones = eventTracker.getZoneStatus(null);
						}
						
						clientConnection.send(JSON.stringify(zones));
					}
					
					else if(action == "facilityStatus")
					{
						if(decodedMessage.worlds != undefined && decodedMessage.worlds.length > 0 && decodedMessage.zones != undefined && decodedMessage.zones.length > 0)
						{
							var facilityStatus =
							{
								"facilityStatus": {}
							};
							
							for(var i=0; i<decodedMessage.worlds.length; i++)
							{
								facilityStatus["facilityStatus"][decodedMessage.worlds[i]] =
								{
									"zones": {}
								};
								
								for(var j=0; j<decodedMessage.zones.length; j++)
								{
									facilityStatus["facilityStatus"][decodedMessage.worlds[i]]["zones"][decodedMessage.zones[j]] = eventTracker.getSelectedRegions(decodedMessage.worlds[i], decodedMessage.zones[j], "0");
								}
							}
							
							clientConnection.send(JSON.stringify(facilityStatus));
						}
					}
					
					else if(eventType in clientConnection.subscriptions)
					{
						var subscriptionData = clientConnection.subscriptions[eventType];
						if(decodedMessage.action == "subscribe")
						{
							for(var property in decodedMessage)
							{
								if(property in subscriptionData)
								{
									if(property == "all")
									{
										if(decodedMessage[property] == "true")
										{
											subscriptionData["all"] = "true";
										}
									}
									else if(property != "worlds" && property != "zones" || (decodedMessage["worlds"] == undefined && property == "zones"))
									{
										for(var i=0; i<decodedMessage[property].length; i++)
										{
											if(subscriptionData[property].indexOf(decodedMessage[property][i]) == -1)
											{
												subscriptionData[property].push(decodedMessage[property][i]);
											}
										}
									}
									else if(property == "worlds")
									{
										for(var i=0; i<decodedMessage[property].length; i++)
										{
											if(!(decodedMessage[property][i] in subscriptionData[property]))
											{
												subscriptionData[property][decodedMessage[property][i]] =
												{
													"zones": []
												}
											}
											
											if(decodedMessage["zones"] != undefined && decodedMessage["zones"].length > 0)
											{
												for(var j=0; j<decodedMessage["zones"].length; j++)
												{
													if(subscriptionData[property][decodedMessage[property][i]].zones.indexOf(decodedMessage["zones"][j]) == -1)
													{
														subscriptionData[property][decodedMessage[property][i]].zones.push(decodedMessage["zones"][j]);
													}
												}
											}
										}
									}
								}
							}
						}
						else if(decodedMessage.action == "unsubscribe")
						{
							if(decodedMessage["all"] == "false")
							{
								subscriptionData["all"] = "false";
							}
							
							for(var property in decodedMessage)
							{
								if(property in subscriptionData && property != "all" && property != "worlds")
								{
									subscriptionData[property] = subscriptionData[property].filter(function(x) { return decodedMessage[property].indexOf(x) < 0 });
								}
								else if(property in subscriptionData && property == "worlds")
								{
									for(var i=0; i<decodedMessage[property].length; i++)
									{
										if(subscriptionData[property][decodedMessage[property][i]] != undefined)
										{
											if(decodedMessage["zones"] == undefined)
											{
												delete subscriptionData[property][decodedMessage[property][i]];
											}
											else
											{
												subscriptionData[property][decodedMessage[property][i]].zones = subscriptionData[property][decodedMessage[property][i]].zones.filter(function(x) { return decodedMessage["zones"].indexOf(x) < 0 });
											}
										}
									}
								}
							}
						}
						else if(decodedMessage.action == "unsubscribeAll")
						{
							for(var subscription in subscriptionData)
							{
								subscriptionData[subscription] = [];
							}
							
							subscriptionData["all"] = "false";
						}
					}
					
					else if(decodedMessage.action == "unsubscribeAll")
					{
						clientConnection.subscriptions = getBlankSubscription();
					}
					
					else
					{
						clientConnection.send('{"error": "unknownAction", "message": "There is no Action or Event Type by that name. Please check your syntax, and try again."}');
					}
					
					if(decodedMessage.action == "subscribe" || decodedMessage.action == "unsubscribe" || decodedMessage.action == "unsubscribeAll")
					{
						var returnObject =
						{
							'subscriptions': {}
						};
						
						for(var subscription in clientConnection.subscriptions)
						{
							for(var property in clientConnection.subscriptions[subscription])
							{
								if((clientConnection.subscriptions[subscription][property].length > 0 && property != "all" && property != "worlds") || (property == "all" && clientConnection.subscriptions[subscription][property] == 'true') || (property == "worlds" && Object.keys(clientConnection.subscriptions[subscription][property]).length > 0))
								{
									returnObject['subscriptions'][subscription] = clientConnection.subscriptions[subscription];
									break;
								}
							}
						}
						
						clientConnection.send(JSON.stringify(returnObject));
					}
				}
			});
			
			clientConnection.on('close', function(code, message)
			{
				console.log((new Date()) + ' User ' + apiUsername + ' disconnected. API Key: ' + apiKey);
				delete clientConnections[clientConnection.id];
			});
		}
		else
		{
			clientConnection.close(); //We should never get here.
		}
	});
});
	
/************************
    Server Functions    *
************************/

//Validate Connecting API Key
function verifyAPIKey(APIKey, callback)
{	
	if(APIKey != undefined && APIKey != null && APIKey != "")
	{
		pool.getConnection(function(err, dbConnection)
		{
			if (err) callback(false, null);
			
			dbConnection.query('SELECT * FROM APIKeys WHERE api_key = ? AND enabled = 1', APIKey, function(err, rows, fields)
			{
				dbConnection.release();
				if(rows.length > 0)
				{
					callback(true, rows[0].name);
				}
				else
				{
					callback(false, null);
				}
			});
		});
	}
	else
	{
		callback(false);
	}
}

// Broadcast Event to all open clientConnections
exports.broadcastEvent = function(rawData)
{
	var messageToSend = {};
	
	messageToSend.payload = rawData.messageData;
	messageToSend.event_type = rawData.eventType;

	Object.keys(clientConnections).forEach(function(key)
	{
		var clientConnection = clientConnections[key];
		if(clientConnection.readyState == 1)
		{
			var subscriptionProperties = clientConnection.subscriptions[rawData.eventType];
			
			if(subscriptionProperties["show"].length > 0 || subscriptionProperties["hide"].length > 0)
			{
				messageToSend.payload = {};
				for(var field in rawData.messageData)
				{
					if(subscriptionProperties["show"].indexOf(field) >= 0)
					{
						messageToSend.payload[field] = rawData.messageData[field];
					}
				}
				
				for(var field in messageToSend.payload)
				{
					if(subscriptionProperties["hide"].indexOf(field) >= 0)
					{
						delete(messageToSend.payload[field]);
					}
				}
			}
			
			if(subscriptionProperties["all"] == "true")
			{
				clientConnection.send(JSON.stringify(messageToSend));
			}
			
			else
			{
				var sendMessage;
				
				for(var property in subscriptionProperties)
				{
					if(property != "all" && property != "useAND" && property != "show" && property != "hide")
					{
						var filterData = rawData.filterData[property];
						
						if(property == "worlds")
						{
							if(subscriptionProperties[property][filterData] != undefined)
							{
								var zoneData = rawData.filterData["zones"];
								
								if(subscriptionProperties[property][filterData].zones.indexOf(zoneData[0]) >= 0)
								{
									sendMessage = true;
								}
								else if(subscriptionProperties[property][filterData].zones.length > 0)
								{
									sendMessage = false;
								}
								else
								{
									sendMessage = true;
								}
								
							}
						}
						
						else
						{
							if(subscriptionProperties["useAND"].indexOf(property) >= 0)
							{
								for(var i=0;i<filterData.length; i++)
								{
									if(subscriptionProperties[property].indexOf(filterData[i]) >= 0)
									{
										sendMessage = true;
									}
									else if(subscriptionProperties[property].length > 0)
									{
										sendMessage = false;
										break;
									}
								}
							}
							
							else
							{
								for(var i=0;i<filterData.length; i++)
								{
									if(subscriptionProperties[property].indexOf(filterData[i]) >= 0)
									{
										sendMessage = true;
										break;
									}
									else if(subscriptionProperties[property].length > 0)
									{
										sendMessage = false;
									}
								}
							}
						}
						
						if(sendMessage == false)
						{
							break;
						}
					}
				}
				
				if(sendMessage == true)
				{
					clientConnection.send(JSON.stringify(messageToSend));
				}
			}
		}
	});
}

exports.currentAlerts = function(alerts)
{
	this.alerts = alerts;
}

function getBlankSubscription()
{
	var blankSubscription =
	{
		'Combat': 
		{
			all: "false",
			worlds: {},
			useAND: [],
			show: [],
			hide: [],
			characters: [],
			outfits: [],
			factions: [],
			loadouts: [],
			vehicles: [],
			weapons: [],
			headshots: [],
			zones: []
		},
		'VehicleCombat':
		{
			all: "false",
			worlds: {},
			useAND: [],
			show: [],
			hide: [],
			characters: [],
			outfits: [],
			factions: [],
			loadouts: [],
			vehicles: [],
			weapons: [],
			facilities: [],
			zones: []
		},
		'Alert':
		{
			all: "false",
			worlds: {},
			useAND: [],
			show: [],
			hide: [],
			alerts: [],
			alert_types: [],
			statuses: [],
			dominations: [],
			zones: [],
			facility_types: [],
		},
		'FacilityControl':
		{
			all: "false",
			worlds: {},
			useAND: [],
			show: [],
			hide: [],
			facilities: [],
			facility_types: [],
			factions: [],
			captures: [],
			zones: []
		},
		'ContinentLock':
		{
			all: "false",
			worlds: {},
			useAND: [],
			show: [],
			hide: [],
			factions: [],
			zones: []
		},
		'BattleRank':
		{
			all: "false",
			worlds: {},
			useAND: [],
			show: [],
			hide: [],
			characters: [],
			outfits: [],
			factions: [],
			battle_ranks: [],
			zones: []
		},
		'Login':
		{
			all: "false",
			worlds: {},
			useAND: [],
			show: [],
			hide: [],
			characters: [],
			outfits: [],
			factions: [],
			login_types: []
		},
		'DirectiveCompleted':
		{
			all: "false",
			worlds: {},
			useAND: [],
			show: [],
			hide: [],
			characters: [],
			outfits: [],
			factions: [],
			directive_tiers: [],
			directive_trees: []
		},
		'AchievementEarned':
		{
			all: "false",
			worlds: {},
			useAND: [],
			show: [],
			hide: [],
			characters: [],
			outfits: [],
			factions: [],
			achievements: [],
			zones: []
		},
		'PlanetsideTime':
		{
			all: "false",
			useAND: [],
			show: [],
			hide: []
		}
	};
	
	return blankSubscription;
}