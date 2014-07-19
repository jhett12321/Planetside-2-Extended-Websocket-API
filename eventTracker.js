//This file processes the raw data provided by SOE's Census REST API, and saves the data to a local database, and a relay Websocket Server.

//TODO:
		//Census Events:
			//Fully Implemented: 						Death
			//Mostly Implemented [No hist. API]: 		VehicleDestroy, MetagameEvent
			//Client Implemented [No Relay/hist. API]:  PlayerLogin, PlayerLogout
			//Not Yet Implemented: 						FacilityControl***, BattleRankUp, ItemAdded, SkillAdded
			//***FacilityControl events are currently use for MetagameEvent updates.

//Includes
var WebSocket = require('ws');
var mysql = require('mysql');
var http = require('http');
var url = require('url');

var eventServer = require('./eventServer.js');
var config = require('./config.js');

//Census/Application status. Turns false if census queries fail, or timeout.
var online = true;

//SOE Census Service ID
var serviceID = config.soeServiceID; 

//MySQL Database Pool. Utilises 100 connections to store raw event data.
var pool = mysql.createPool(
{
	connectionLimit : config.dbConnectionLimit,
	host: config.dbHost,
	user: config.dbUser,
	password: config.dbPassword,
	database: config.dbName,
	supportBigNumbers: true,
	bigNumberStrings: true
});

//Alert Identifiers.
var alertTypes = {
	1:{zone:2, facility:0}, //Indar Territory
	2:{zone:8, facility:0}, //Esamir Territory
	3:{zone:6, facility:0}, //Amerish Territory
	4:{zone:4, facility:3}, //Hossin Territory
	7:{zone:6, facility:3}, //Amerish Bio Lab
	8:{zone:6, facility:4}, //Amerish Tech Plant
	9:{zone:6, facility:2}, //Amerish Amp Station
	10:{zone:2, facility:3}, //Indar Bio Lab
	11:{zone:2, facility:4}, //Indar Tech Plant
	12:{zone:2, facility:2}, //Indar Amp Station
	13:{zone:8, facility:3}, //Esamir Bio Lab
	14:{zone:8, facility:2},  //Esamir Amp Station
	16:{zone:4, facility:3}, //Hossin Bio Lab
	17:{zone:4, facility:4}, //Hossin Tech Plant
	18:{zone:4, facility:2} //Hossin Amp Station
};

//World IDs
var worlds = [1,9,10,11,13,17,19,25];

//Zone IDs
var zones = [2,4,6,8];

//-------------------------------------------------------------------
/**
* 	WEBSOCKET CLIENT
* 	Manages, records and relays information provided by the SOE API.
*/
//-------------------------------------------------------------------
	
/**********************
    Initialisation    *
**********************/

//Cache and Query Queue System - See Census Query Processor
var characters = {}; //Used for combat events.
var queriesToProcess = []; //Census Character Query Queue
var failureCount = 0;
var maxFailures = 10;

//Alerts
var alerts = {};
exports.alerts = alerts;	 //Used for tracking alert facility control.

//Regions
var regions = {};

//Websocket Client. Connects to SOE's Census REST API.
var wsClient = new persistentClient();


function init(callback)
{
	//Reset Failure Count
	failureCount = 0;
	
	//Region Data
	for(var i=0; i<worlds.length; i++)
	{
		var world = worlds[i] + 0;
		regions[world] = {};
		
		setTimeout(initRegionData(world), i * 1000);
	}
	
	//Update Alert Data
	setTimeout(function()
	{
		if(online)
		{
			var timestamp = Math.round(Date.now() / 1000) - 7201;
			GetCensusData("http://census.soe.com/s:" + serviceID + "/get/ps2:v2/world_event/?type=METAGAME&c:limit=1000&c:lang=en&after=" + timestamp, function(success, data)
			{
				if(success)
				{
					var finishedAlerts = [];
					
					for(var i = 0; i < data.world_event_list.length; i++)
					{
						var alert = data.world_event_list[i];
						if(alert.metagame_event_state == 137 || alert.metagame_event_state == 138)
						{
							finishedAlerts.push(alert.instance_id);
						}
					}
					
					pool.getConnection(function(err, dbConnection)
					{
						dbConnection.query('DELETE FROM AlertEvents WHERE status = 1', function(err, result)
						{
							if (err) throw err;
							dbConnection.release();
						});
					});
					
					for(var i = 0; i < data.world_event_list.length; i++)
					{
						var alert = data.world_event_list[i];
						if((alert.metagame_event_state == 135 || alert.metagame_event_state == 136) && finishedAlerts.indexOf(alert.instance_id) < 0)
						{
							//Dummy Alert Message
							var dummyMessage =
							{
								"payload":
								{
									"event_name": "MetagameEvent",
									"instance_id": alert.instance_id,
									"metagame_event_id": alert.metagame_event_id,
									"metagame_event_state": alert.metagame_event_state,
									"timestamp": alert.timestamp,
									"world_id": alert.world_id
								},
								"service":"event",
								"type":"serviceMessage"
							};
							processMessage(JSON.stringify(dummyMessage));
						}
					}
					
					callback();
				}
			});
		}
	}, worlds.length * 1000);
}

function initRegionData(world)
{
	//Region Data
	GetCensusData("http://census.soe.com/s:" + serviceID + "/get/ps2:v2/map?world_id=" + world + "&zone_ids=" + zones.join(",") + "&c:join=map_region^on:Regions.Row.RowData.RegionId^to:map_region_id^inject_at:map_region^show:facility_id'facility_type_id'map_region_id'location_x'location_z(map_hex^on:map_region_id^to:map_region_id^list:1^show:x'y^inject_at:hex)", function(success, data)
	{
		if(success)
		{
			for(var j=0; j<data.map_list.length; j++)
			{
				var zoneData = data.map_list[j];
				var zoneID = zoneData.ZoneId;
				
				for(var k=0; k<zoneData.Regions.Row.length; k++)
				{
					var regionData = zoneData.Regions.Row[k].RowData;
					var facilityID = regionData.map_region.facility_id;
					
					var regionInfo = {
						'facility_type_id': regionData.map_region.facility_type_id,
						'hex_data': regionData.map_region.hex,
						'owner': regionData.FactionId,
						'location_x': regionData.map_region.location_x,
						'location_y': regionData.map_region.location_z,
						'zone_id': zoneID
					};
					
					regions[world][facilityID] = regionInfo;
				}
			}
		}
	});
}

/****************
    Services    *
****************/
	
//Connection Watcher - Reconnects if websocket connection is dropped.
function watcher()
{
	if(!wsClient.isConnected())
	{
		if(online)
		{
			console.log('Reconnecting...');
			wsClient = new persistentClient();
		}
	}
}

//Census Query Processor
function censusProcessor()
{
	var queries = queriesToProcess.splice(0);
	
	if(queries.length > 0)
	{
		var charList = [];
		for(var query in queries)
		{
			if(queries.hasOwnProperty(query))
			{
				charList = charList.concat(queries[query].charList);
			}
		}
		
		var url = "http://census.soe.com/s:" + serviceID + "/get/ps2:v2/character?character_id=" + charList.join(",") + "&c:show=character_id,faction_id&c:join=outfit_member^on:character_id^to:character_id^show:outfit_id";
		GetCensusData(url, function(success, data)
		{
			if(success)
			{
				for(var j = 0; j < charList.length; j++)
				{
					characters[charList[j]] =
					{
						outfitID: 0,
						factionID: 0
					};
				}
				
				for(var i = 0; i < data.character_list.length; i++)
				{
					var character = data.character_list[i];
					
					for(var j = 0; j < charList.length; j++)
					{
						if(character.character_id == charList[j])
						{
							var characterFactionID = character.faction_id;
							var characterOutfitID = 0;
							if(character.character_id_join_outfit_member != null)
							{
								characterOutfitID = character.character_id_join_outfit_member.outfit_id;
							}
						
							characters[charList[j]].outfitID = characterOutfitID;
							characters[charList[j]].factionID = characterFactionID;
						}
					}
				}
				
				var callbacks = [];
				for(var query in queries)
				{
					if(queries.hasOwnProperty(query))
					{
						callbacks.push(queries[query].callback);
					}
				}
				
				var count = 0;
				callbacks.forEach(function(callback)
				{
					count++;
					callback(true);
					if(count == callbacks.length)
					{
						queries = null;
					}
				});
			}
			else
			{
				var callbacks = [];
				for(var query in queries)
				{
					if(queries.hasOwnProperty(query))
					{
						charList = charList.concat(queries[query].callback);
					}
				}
				
				var count = 0;
				callbacks.forEach(function(callback)
				{
					count++;
					callback(false);
					if(count == callbacks.length)
					{
						queries = null;
					}
				});
			}
		});
	}
}
setInterval(watcher, 3000);
setInterval(censusProcessor, 1000);

/**************
    Client    *
**************/

function persistentClient()
{
	var client;
	var connected = true;
	
	//Return Status of connection.
	this.isConnected = function()
	{
		return connected;	
	}
	
	//Returns the client associated with this connection.
	this.GetClient = function()
	{
		if(client != undefined)
		{
			return client;
		}
		else
		{
			return undefined;
		}
	}
	
	init(function()
	{
		client = new WebSocket('wss://push.planetside2.com/streaming?service-id=s:' + serviceID);
			
		//Events
		client.on('open', function()
		{
			console.log((new Date()) + ' WebSocket client connected!');
			//Uncomment when zones/alerts aren't borked.
			var messageToSend = '{"service":"event","action":"subscribe","characters":["all"],"worlds":["all"],"eventNames":["Death","FacilityControl","MetagameEvent","PlayerLogin","PlayerLogout","VehicleDestroy"]}';
			//var messageToSend = '{"service":"event","action":"subscribe","characters":["all"],"worlds":["all"],"eventNames":["Death","PlayerLogin","PlayerLogout","VehicleDestroy"]}';
			client.send(messageToSend);
		});
		
		client.on('message', function(data, flags)
		{
			processMessage(data);
		});
		client.on('error', function(error)
		{
			console.log((new Date()) + ' Connect Error: ' + error.toString());
			connected = false;
		});
		client.on('close', function(code)
		{
			console.log((new Date()) + ' Websocket Connection Closed [' + code +']');
			connected = false;
		});
	});
}

/************************
    Client Functions    *
************************/

//Processes Messages received from the client.
function processMessage(messageData)
{
	var message = JSON.parse(messageData);
	
	if(message.service == "event" && message.type == "serviceMessage" && message.payload != undefined)
	{
		var payload = message.payload;
		var eventType = payload.event_name;
		
		//Combat and Vehicle Combat
		if(eventType == "Death" || eventType == "VehicleDestroy")
		{
			if(isValidZone(payload.zone_id))
			{
				var attackerCharacterID = payload.attacker_character_id;
				var characterID = payload.character_id;
				
				if(isValidCharacter(characterID) || isValidCharacter(attackerCharacterID))
				{
					if(!isValidCharacter(characterID))	
					{
						characterID = attackerCharacterID;
					}
					if(!isValidCharacter(attackerCharacterID))
					{
						attackerCharacterID = characterID;
					}
					
					var eventCharacters = [characterID, attackerCharacterID];
				
					retrieveCharacterInfo(eventCharacters, function(success)
					{
						if(success)
						{
							var characterOutfitID = characters[characterID].outfitID;
							var characterFactionID = characters[characterID].factionID;
						
							var attackerOutfitID = characters[attackerCharacterID].outfitID;
							var attackerFactionID = characters[attackerCharacterID].factionID;
							
							var type = "";
							var post = {};
							var filterData = {};
							
							if(eventType == "Death")
							{
								type = "Combat";
								post =
								{
									attacker_character_id: attackerCharacterID,
									attacker_outfit_id: attackerOutfitID,
									attacker_faction_id: attackerFactionID,
									attacker_loadout_id: payload.attacker_loadout_id,
									victim_character_id: characterID,
									victim_outfit_id: characterOutfitID,
									victim_faction_id: characterFactionID,
									victim_loadout_id: payload.character_loadout_id,
									timestamp: payload.timestamp,
									weapon_id: payload.attacker_weapon_id,
									vehicle_id: payload.attacker_vehicle_id,
									headshot: payload.is_headshot,
									zone_id: payload.zone_id,
									world_id: payload.world_id
								};
								
								filterData = 
								{
									characters: [attackerCharacterID, characterID],
									outfits: [attackerOutfitID,characterOutfitID],
									factions: [attackerFactionID,characterFactionID],
									loadouts: [payload.attacker_loadout_id,payload.character_loadout_id],
									vehicles: [payload.attacker_vehicle_id],
									weapons: [payload.attacker_weapon_id],
									headshots: [payload.is_headshot],
									zones: [payload.zone_id],
									worlds: [payload.world_id]
								}
								
								pool.getConnection(function(err, dbConnection)
								{
									dbConnection.query('INSERT INTO CombatEvents SET ?', post, function(err, result)
									{
										if (err) throw err;
										dbConnection.release();
									});
								});
							}
							else if(eventType == "VehicleDestroy")
							{
								type = "VehicleCombat";
								post =
								{
										attacker_character_id: attackerCharacterID,
										attacker_outfit_id: attackerOutfitID,
										attacker_faction_id: attackerFactionID,
										attacker_vehicle_id: payload.attacker_vehicle_id,
										victim_character_id: characterID,
										victim_outfit_id: characterOutfitID,
										victim_faction_id: characterFactionID,
										victim_vehicle_id: payload.vehicle_id,
										timestamp: payload.timestamp,
										weapon_id: payload.attacker_weapon_id,
										zone_id: payload.zone_id,
										world_id: payload.world_id
								};
								filterData = 
								{
									characters: [attackerCharacterID, characterID],
									outfits: [attackerOutfitID,characterOutfitID],
									factions: [attackerFactionID,characterFactionID],
									loadouts: [payload.attacker_loadout_id],
									vehicles: [payload.attacker_vehicle_id,payload.vehicle_id],
									weapons: [payload.attacker_weapon_id],
									zones: [payload.zone_id],
									worlds: [payload.world_id]
								}
								
								pool.getConnection(function(err, dbConnection)
								{
									dbConnection.query('INSERT INTO VehicleCombatEvents SET ?', post, function(err, result)
									{
										if (err) throw err;
										dbConnection.release();
									});
								});
							}
							
							var eventData =
							{
								eventType: type,
								messageData: post,
								filterData: filterData
							};
								
							eventServer.broadcastEvent(eventData);
						}
					});
				}
			}
		}
		
		//Alerts and Territory Control
		else if(eventType == "MetagameEvent" || eventType == "FacilityControl")
		{
			if(eventType == "FacilityControl")
			{
				if(isValidZone(payload.zone_id))
				{
					var worldID = payload.world_id;
					var facilityID = payload.facility_id;
					
					regions[worldID][facilityID].owner = payload.new_faction_id;
					
					calculateTerritoryControl(worldID, payload.zone_id, 0, function(controlVS, controlNC, controlTR, majorityController)
					{
						var post =
						{
							facility_id: payload.facility_id,
							duration_held: payload.duration_held,
							new_faction_id: payload.new_faction_id,
							old_faction_id: payload.old_faction_id,
							timestamp: payload.timestamp,
							zone_id: payload.zone_id,
							location_x: regions[worldID][facilityID].location_x,
							location_y: regions[worldID][facilityID].location_y,
							hex_data: JSON.stringify(regions[worldID][facilityID].hex_data),
							control_vs: controlVS,
							control_nc: controlNC,
							control_tr: controlTR,
							majority_controller: majorityController,
							world_id: payload.world_id
						};
						
						pool.getConnection(function(err, dbConnection)
						{
							dbConnection.query('INSERT INTO FacilityControlEvents SET ?', post, function(err, result)
							{
								if (err) throw err;
								dbConnection.release();
							});
						});
					});
				}
			}
			
			//Process Event-Specific Data
			else if(eventType == "MetagameEvent")
			{
				if(payload.metagame_event_state == 135 || payload.metagame_event_state == 136)
				{
					retrieveAlertInfo(payload.instance_id, payload.metagame_event_id, payload.world_id, function(uID, controlVS, controlNC, controlTR, majorityController)
					{
						alerts[uID].start_time = payload.timestamp;
						
						var post =
						{
							alert_id: payload.instance_id,
							alert_type_id: payload.metagame_event_id,
							start_time: payload.timestamp,
							end_time: 0,
							status: 1,
							control_vs: controlVS,
							control_nc: controlNC,
							control_tr: controlTR,
							majority_controller: majorityController,
							domination: 0,
							zone_id: alertTypes[payload.metagame_event_id].zone,
							facility_type_id: alertTypes[payload.metagame_event_id].facility,
							world_id: payload.world_id
						};
						
						var messageData =
						{
							alert_id: payload.instance_id,
							alert_type_id: payload.metagame_event_id,
							start_time: payload.timestamp,
							end_time: 0,
							status: 1,
							control_vs: controlVS,
							control_nc: controlNC,
							control_tr: controlTR,
							majority_controller: majorityController,
							domination: 0,
							zone_id: alertTypes[payload.metagame_event_id].zone,
							facility_type_id: alertTypes[payload.metagame_event_id].facility,
							world_id: payload.world_id
						}
						
						var filterData = 
						{
							alerts: [payload.instance_id],
							alert_types: [payload.metagame_event_id],
							statuses: [1],
							dominations: [0],
							zones: [alertTypes[payload.metagame_event_id].zone],
							facilityTypes: [alertTypes[payload.metagame_event_id].facility],
							worlds: [payload.world_id]
						}
						
						pool.getConnection(function(err, dbConnection)
						{
							dbConnection.query('INSERT INTO AlertEvents SET ?', post, function(err, result)
							{
								if (err) throw err;
								dbConnection.release();
							});
						});
						
						var eventData =
						{
							eventType: "Alert",
							messageData: messageData,
							filterData: filterData
						};
						
						eventServer.broadcastEvent(eventData);
					});
				}
				
				else if(payload.metagame_event_state == 137 || payload.metagame_event_state == 138)
				{
					var zoneID = alertTypes[payload.metagame_event_id].zone;
					var facilityTypeID = alertTypes[payload.metagame_event_id].facility;
					
					calculateTerritoryControl(payload.world_id, zoneID, facilityTypeID , function(controlVS, controlNC, controlTR, majorityController)
					{
						var uID = payload.world_id + "_" + payload.instance_id;
						
						var domination = 0;
						if(controlVS >= 94 || controlNC >= 94 || controlTR >= 94)
						{
							domination = 1;
						}
						
						var post =
						{
							end_time: payload.timestamp,
							status: 0,
							control_vs: controlVS,
							control_nc: controlNC,
							control_tr: controlTR,
							majority_controller: majorityController,
							domination: domination
						};
						
						var messageData =
						{
							alert_id: payload.instance_id,
							alert_type_id: payload.metagame_event_id,
							start_time: alerts[uID].start_time,
							end_time: payload.timestamp,
							status: 0,
							control_vs: controlVS,
							control_nc: controlNC,
							control_tr: controlTR,
							majority_controller: majorityController,
							domination: domination,
							zone_id: alertTypes[payload.metagame_event_id].zone,
							facility_type_id: alertTypes[payload.metagame_event_id].facility,
							world_id: payload.world_id
						}
						
						var filterData = 
						{
							alerts: [payload.instance_id],
							alert_types: [payload.metagame_event_id],
							statuses: [0],
							dominations: [domination],
							zones: [alertTypes[payload.metagame_event_id].zone],
							facilityTypes: [alertTypes[payload.metagame_event_id].facility],
							worlds: [payload.world_id]
						}
						
						var alertID = payload.instance_id;
						var sql = 'UPDATE AlertEvents SET ? WHERE alert_id = ' + alertID + ' AND world_id = ' + payload.world_id;
						pool.getConnection(function(err, dbConnection)
						{
							dbConnection.query(sql, post, function(err, result)
							{
								if (err) throw err;
								dbConnection.release();
							});
						});	
						
						var eventData =
						{
							eventType: "Alert",
							messageData: messageData,
							filterData: filterData
						};
						
						eventServer.broadcastEvent(eventData);
						
						var uID = payload.world_id + "_" + alertID;
						delete alerts[uID];
						
						//Add Entry to Continent Locks
						if(majorityController != 0)
						{
							var lockPost =
							{
								zone_id: alertTypes[payload.metagame_event_id].zone,
								world_id: payload.world_id,
								timestamp: payload.timestamp,
								locked_by: majorityController,
								lock_type: 1
							};
							pool.getConnection(function(err, dbConnection)
							{
								dbConnection.query('INSERT INTO ContinentLockEvents SET ?', lockPost, function(err, result)
								{
									if (err) throw err;
									dbConnection.release();
								});
							});
						}
					});
				}
			}
			
			if(eventType == "FacilityControl")
			{
				if(isValidZone(payload.zone_id) && payload.old_faction_id != payload.new_faction_id)
				{
					var uID = null;
					for (var alert in alerts)
					{
						if (alerts.hasOwnProperty(alert))
						{
							if(parseInt(alerts[alert].world_id) == parseInt(payload.world_id))
							{
								uID = alert;
								break;
							}
						}
					}
					
					if(uID != null)
					{
						var zoneID = alertTypes[alerts[uID].alert_type_id].zone;
						var facilityTypeID = alertTypes[alerts[uID].alert_type_id].facility;
						
						calculateTerritoryControl(payload.world_id, zoneID, facilityTypeID, function(controlVS, controlNC, controlTR, majorityController)
						{
							var post =
							{
								control_vs: controlVS,
								control_nc: controlNC,
								control_tr: controlTR
							};
							
							var alertID = alerts[uID].alert_id;
							var alertTypeID = alerts[uID].alert_type_id;
							
							var messageData =
							{
								alert_id: alertID,
								alert_type_id: alertTypeID,
								start_time: alerts[uID].start_time,
								end_time: 0,
								timestamp: payload.timestamp,
								status: 2,
								control_vs: controlVS,
								control_nc: controlNC,
								control_tr: controlTR,
								domination: 0,
								zone_id: alertTypes[alertTypeID].zone,
								facility_type_id: alertTypes[alertTypeID].facility,
								world_id: payload.world_id
							}
							
							var filterData = 
							{
								alerts: [alertID],
								alert_types: [alertTypeID],
								statuses: [2],
								dominations: [0],
								zones: [alertTypes[alertTypeID].zone],
								facilityTypes: [alertTypes[alertTypeID].facility],
								worlds: [payload.world_id]
							}
							
							var sql = 'UPDATE AlertEvents SET ? WHERE alert_id = ' + alertID + ' AND world_id = ' + payload.world_id;
							pool.getConnection(function(err, dbConnection)
							{
								dbConnection.query(sql, post, function(err, result)
								{
									if (err) throw err;
									dbConnection.release();
								});
							});
							
							var eventData =
							{
								eventType: "Alert",
								messageData: messageData,
								filterData: filterData
							};
							
							eventServer.broadcastEvent(eventData);
						});
					}
					
					calculateTerritoryControl(payload.world_id, payload.zone_id, 0, function(controlVS, controlNC, controlTR, majorityController)
					{
						if(controlVS >= 90 || controlNC >= 90 || controlTR >= 90)
						{
							var facilityID = payload.facility_id;
							updateRegions(payload.world_id, payload.zone_id, function(success)
							{
								if(success)
								{
									calculateTerritoryControl(payload.world_id, payload.zone_id, 0, function(controlVS, controlNC, controlTR, majorityController)
									{
										if(controlVS == 100 || controlNC == 100 || controlTR == 100)
										{
											post =
											{
												zone_id: payload.zone_id,
												world_id: payload.world_id,
												timestamp: payload.timestamp,
												locked_by: majorityController,
												lock_type: 2
											};
											pool.getConnection(function(err, dbConnection)
											{
												dbConnection.query('INSERT INTO ContinentLockEvents SET ?', post, function(err, result)
												{
													if (err) throw err;
													dbConnection.release();
												});
											});
										}
									});
								}
							});
						}
					});
				}
			}
		}
		
		else if(eventType == "PlayerLogin" || eventType == "PlayerLogout")
		{
			var isLogin = 0;
			if(eventType == "PlayerLogin")
			{
				isLogin = 1;
			}
			var post =
			{
				character_id: payload.character_id,
				is_login: isLogin,
				timestamp: payload.timestamp,
				world_id: payload.world_id
			};
			
			pool.getConnection(function(err, dbConnection)
			{
				dbConnection.query('INSERT INTO LoginEvents SET ?', post, function(err, result)
				{
					if (err) throw err;
					dbConnection.release();
				});
			});
		}
	}
	else
	{
		console.log((new Date()) + " CENSUS MESSAGE: " + messageData);
	}
}

//Checks if the given zone is valid.
function isValidZone(zoneID)
{
	if(zoneID != undefined && zoneID != null)
	{
		if(parseInt(zoneID) < 90)
		{		
			return true;	
		}
	}
	return false;
}

//Checks if the given character is valid.
function isValidCharacter(characterID)
{
	if(characterID != undefined && characterID != null)
	{
		if(characterID.length == 19)
		{		
			return true;	
		}
	}
	return false;
}

//Adds the given characters to the query processor. The callback is called once the character's outfit, and faction have been resolved.
function retrieveCharacterInfo(charList, callback)
{
	var query =
	{
		callback: callback,
		charList: charList
	};
	queriesToProcess.push(query);
}

function calculateTerritoryControl(worldID, zoneID, facilityTypeID, callback)
{
	var totalRegions = 0;
	var facilitiesVS = 0;
	var facilitiesNC = 0;
	var facilitiesTR = 0;
	
	for(var region in regions[worldID])
	{
		if(regions[worldID][region].facility_type_id != 7)
		{
			if((regions[worldID][region].zone_id == zoneID && facilityTypeID == 0) || (zoneID == 0 && regions[worldID][region].facility_type_id == facilityTypeID) || (regions[worldID][region].zone_id == zoneID && regions[worldID][region].facility_type_id == facilityTypeID))
			{
				totalRegions++;
				if(regions[worldID][region].owner == 1)
				{
					facilitiesVS++;
				}
				else if(regions[worldID][region].owner == 2)
				{
					facilitiesNC++;
				}
				else if(regions[worldID][region].owner == 3)
				{
					facilitiesTR++;
				}
			}
		}
	}
	
	var controlVS = Math.floor(facilitiesVS / totalRegions * 100);
	var controlNC = Math.floor(facilitiesNC / totalRegions * 100);
	var controlTR = Math.floor(facilitiesTR / totalRegions * 100);
	
	var majorityControl = controlVS;
	var majorityController = 1;
	
	if(controlNC > majorityControl)
	{
		majorityControl = controlNC;
		majorityController = 2;
	}
	else if(controlNC == majorityControl)
	{
		majorityController = 0;
	}
	
	if(controlTR > majorityControl)
	{
		majorityControl = controlTR;
		majorityController = 3;
	}
	else if(controlTR == majorityControl)
	{
		majorityController = 0;
	}
	
	callback(controlVS, controlNC, controlTR, majorityController);
}

//Updates all regions on a given continent. Required since Census doesn't send events for continent locks.
function updateRegions(worldID, zoneIDs, callback)
{
	var url = "http://census.soe.com/s:" + serviceID + "/get/ps2:v2/map?world_id=" + worldID + "&zone_ids=" + zoneIDs + "&c:join=map_region^on:Regions.Row.RowData.RegionId^to:map_region_id^show:facility_id'facility_type_id^inject_at:map_region";
	GetCensusData(url, function(success, data)
	{
		if(success)
		{
			for(var i=0;i<data.map_list[0].Regions.Row.length;i++)
			{
				var region = data.map_list[0].Regions.Row[i].RowData;
				var facilityID = region.map_region.facility_id;
				
				regions[worldID][facilityID].owner = region.FactionId;
			}
		}
		callback(success);
	});
}

//Processes the Alert Census Request. The callback is called once the alert's facilities, and the facility owners have been resolved.
function retrieveAlertInfo(alertID, alertTypeID, worldID, callback)
{
	if(alertTypes[alertTypeID] != undefined && worldID != undefined)
	{
		var uID = worldID + "_" + alertID;
		alerts[uID] =
		{
			alert_id: alertID,
			world_id: worldID,
			start_time: 0,
			alert_type_id: alertTypeID
		}
		
		var zoneID = alertTypes[alertTypeID].zone;
		var facilityTypeID = alertTypes[alertTypeID].facility;
		
		calculateTerritoryControl(worldID, zoneID, facilityTypeID, function(controlVS, controlNC, controlTR, majorityController)
		{
			callback(uID, controlVS, controlNC, controlTR, majorityController);
		});
	}
}

//A Utility function. Will poll the census API until the requested data is received.
function GetCensusData(url, callback)
{
	http.get(url, function(res)
	{
		var body = '';
		res.on('data', function(chunk)
		{
			body += chunk;
		});
		
		res.on('end', function()
		{
			var data = null;
			try
			{
				data = JSON.parse(body);
			}
			catch(error)
			{
				if(failureCount >= maxFailures)
				{
					if(online)
					{
						console.log("SEVERE: [Census Connection Error] - Connection Attempt Limit Reached. Entering Offline Mode. Will try reconnect in 5 minutes.");
						EnterOfflineMode();
					}
					callback(false, null);
				}
				else
				{
					if(online)
					{
						console.log("WARNING: [Census Connection Error] - Failed Attempt " + failureCount);
					}
					failureCount ++;
					GetCensusData(url, callback);
				}
			}
			
			if(data != null && data.returned != undefined && data.returned != null && data.returned != 0)
			{
				callback(true, data);
			}
			
			else
			{
				if(failureCount >= maxFailures)
				{
					if(online)
					{
						console.log("SEVERE: [Census Connection Error] - Connection Attempt Limit Reached. Entering Offline Mode. Will try reconnect in 5 minutes.");
						EnterOfflineMode();
					}
					callback(false, null);
				}
				else
				{
					if(online)
					{
						console.log("WARNING: [Census Connection Error] - Failed Attempt " + failureCount);
					}
					failureCount++;
					GetCensusData(url, callback);
				}
			}
		});
	}).on('error', function(e)
	{
		if(failureCount >= maxFailures)
		{
			if(online)
			{
				console.log("SEVERE: [Census Connection Error] - Connection Attempt Limit Reached. Entering Offline Mode. Will try reconnect in 5 minutes.");
				EnterOfflineMode();
			}
			callback(false, null);
		}
		else
		{
			if(online)
			{
				console.log("WARNING: [Census Connection Error] - Failed Attempt " + failureCount);
			}
			failureCount++;
			GetCensusData(url, callback);
		}
	});
}

//This closes all connections with Census.
function EnterOfflineMode()
{
	if(wsClient.GetClient() != undefined)
	{
		wsClient.GetClient().close();
	}
	online = false;
	
	//Test all of our required queries....
	setInterval(function()
	{
		GetCensusData("http://census.soe.com/s:" + serviceID + "/get/ps2:v2/world_event/?type=METAGAME&c:limit=10&c:lang=en", function(success, data)
		{
			if(success)
			{
				GetCensusData("http://census.soe.com/s:" + serviceID + "/get/ps2:v2/character?character_id=5428010917272162465&c:show=character_id,faction_id&c:join=outfit_member^on:character_id^to:character_id^show:outfit_id", function(success2, data2)
				{
					if(success2)
					{
						GetCensusData("/get/ps2:v2/map?world_id=25&zone_ids=2&c:join=map_region^on:Regions.Row.RowData.RegionId^to:map_region_id^inject_at:map_region^show:facility_id'facility_type_id'map_region_id(map_hex^on:map_region_id^to:map_region_id^list:1^show:x'y^inject_at:hex)", function(success3, data3)
						{
							if(success3)
							{
								online = true;
							}
							else
							{
								console.log("Error: Census Query for map failed. Retrying in 5 Minutes.");
							}
						});
					}
					else
					{
						console.log("Error: Census Query for character failed. Retrying in 5 Minutes.");
					}
				});
			}
			else
			{
				console.log("Error: Census Query for world_event failed. Retrying in 5 Minutes.");
			}
		});
	}, 300000);
}