//Includes
var WebSocketServer = require('websocket').server; //TODO Move into new library.
var http = require('http');
var url = require('url');
var mysql = require('mysql');

var eventTracker = require('./eventTracker.js');
var config = require('./config.js');

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
var server = http.createServer(function(request, response)
{
	response.writeHead(404);
	response.end();
});

var clientConnections = {}; //Stores all connections to this server, and their subscribed events.
var alerts = {}; //Stores all active alerts
var connectionIDCounter = 0; //Connection Unique ID's.

server.listen(8080, function()
{
	console.log((new Date()) + ' Websocket Server is listening on port 8080');
});

var wsServer = new WebSocketServer(
{
	httpServer: server,
	autoAcceptclientConnections: false
});
	
/**************
    Events    *
**************/
	
wsServer.on('request', function(request)
{	
	var apiKey = request.resourceURL.query.apikey;
	
	verifyAPIKey(apiKey, function(isValid, errorMsg) //Verify this client's API Key.
	{
		if(!isValid)
		{
			request.reject(1008, errorMsg); // Make sure we only accept requests from a valid API Key
		}
		else
		{
			var clientConnection = request.accept(null, request.origin);

			// Store a reference to the connection using an incrementing ID
			clientConnection.id = connectionIDCounter ++;
			
			// Store references relating to this connection's subscriptions
			clientConnection.subscriptions = getBlankSubscription();
			
			clientConnections[clientConnection.id] = clientConnection;
		
			console.log((new Date()) + ' Connection ID ' + clientConnection.id + ' accepted.');
			
			//Events
			clientConnection.on('message', function(message)
			{
				try
				{
					var decodedMessage = JSON.parse(message.utf8Data);
					var eventType = decodedMessage.event;
					
					if(eventType in clientConnection.subscriptions)
					{
						var subscriptionData = clientConnection.subscriptions[eventType];
						if(decodedMessage.action == "subscribe")
						{
							if(decodedMessage["all"] == "true")
							{
								subscriptionData["all"] = "true";
							}
							else
							{
								for(var property in decodedMessage) //TODO Get Key of Data.
								{
									if(property in subscriptionData)
									{
										subscriptionData[property] = subscriptionData[property].concat(decodedMessage[property]);
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
							else
							{
								for(var property in decodedMessage) //TODO Get Key of Data.
								{
									if(property in subscriptionData)
									{
										subscriptionData[property] = subscriptionData[property].filter(function(x) { return decodedMessage[property].indexOf(x) < 0 });
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
						
						else if(eventType == "Alert" && decodedMessage.action == "activeAlerts")
						{
							clientConnection.sendUTF(JSON.stringify(eventTracker.alerts));
						}
					}
					
					else if(decodedMessage.action == "unsubscribeAll")
					{
						clientConnection.subscriptions = getBlankSubscription();
					}
				}
				catch(exception)
				{
					clientConnection.sendUTF('{"error": "BADJSON", "message": "You have supplied an invalid JSON string. Please check your syntax."}');
				}
				
				var returnObject =
				{
					'subscriptions': {}
				};
				
				for(var subscription in clientConnection.subscriptions)
				{
					for(var property in clientConnection.subscriptions[subscription])
					{
						if((clientConnection.subscriptions[subscription][property].length > 0 && property != "all") || (property == "all" && clientConnection.subscriptions[subscription][property] == 'true'))
						{
							returnObject['subscriptions'][subscription] = clientConnection.subscriptions[subscription];
							break;
						}
					}
				}
				
				clientConnection.sendUTF(JSON.stringify(returnObject));
			});
			
			clientConnection.on('close', function(reasonCode, description)
			{
				console.log((new Date()) + ' Peer ' + clientConnection.remoteAddress + ' disconnected. ' + "Connection ID: " + clientConnection.id);
				delete clientConnections[clientConnection.id];
			});
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
			if (err) callback(false, "Invalid API Key Specified");
			
			dbConnection.query('SELECT * FROM APIKeys WHERE api_key = ? AND enabled = 1', APIKey, function(err, rows, fields)
			{
				if(rows.length > 0)
				{
					callback(true, "");
				}
				else
				{
					callback(false, "Invalid API Key Specified.");
				}
			});
		});
	}
	else
	{
		callback(false, "No API Key Specified.");
	}
}

// Broadcast Event to all open clientConnections
exports.broadcastEvent = function(rawData)
{
	Object.keys(clientConnections).forEach(function(key)
	{
		var clientConnection = clientConnections[key];
		if (clientConnection.connected)
		{
			var subscriptionProperties = clientConnection.subscriptions[rawData.eventType];
			if(subscriptionProperties["all"] == "true")
			{
				clientConnection.send(JSON.stringify(rawData.messageData));
			}
			else
			{
				for(var property in subscriptionProperties)
				{
					if(property != "all")
					{
						var filterData = rawData.filterData[property];
						
						for(var i=0;i<filterData.length; i++)
						{
							if(subscriptionProperties[property].indexOf(filterData[i]) >= 0)
							{
								clientConnection.send(JSON.stringify(rawData.messageData));
								break;
							}
						}
					}
				}
			}
		}
	});
}

// Send a message to a connection by its connectionID
exports.sendToConnectionId = function(connectionID, data)
{
	var clientConnection = clientConnections[connectionID];
	if (clientConnection && clientConnection.connected)
	{
		clientConnection.send(data);
	}
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
			characters: [],
			outfits: [],
			factions: [],
			loadouts: [],
			vehicles: [],
			weapons: [],
			headshots: [],
			zones: [],
			worlds: []
		},
		'VehicleCombat':
		{
			all: "false",
			characters: [],
			outfits: [],
			factions: [],
			loadouts: [],
			vehicles: [],
			weapons: [],
			zones: [],
			worlds: []
		},
		'Alert':
		{
			all: "false",
			alerts: [],
			alert_types: [],
			statuses: [],
			dominations: [],
			zones: [],
			facilityTypes: [],
			worlds: []
		}
	};
	
	return blankSubscription;
}