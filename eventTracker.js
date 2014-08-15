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

//Modules
var WebSocket = require('ws');
var mysql = require('mysql');
var http = require('http');
var url = require('url');

//Event Tracker Components
var eventServer;
var config = require('./config.js');
var gameData = require('./gameData.js');

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

//Planetside 2 Game Data
//See gameData.js for explanation of values.
var loadoutsVS = gameData.loadoutsVS;
var loadoutsNC = gameData.loadoutsNC;
var loadoutsTR = gameData.loadoutsTR;

var alertTypes = gameData.alertTypes;

var worlds = gameData.worlds;
var zones = gameData.zones;

//-------------------------------------------------------------------
/**
* 	WEBSOCKET CLIENT
* 	Manages, records and relays information provided by the SOE API.
*/
//-------------------------------------------------------------------

/**********************
    Initialisation    *
**********************/

//API status. Remains false while websocket is offline.
var online = false;

//Tracker Start time. Used for events
var startTime = Math.round(Date.now() / 1000);

//Cache and Query Queue System
var characters = {};
var queriesToProcess = [];
var maxFailures = 10;

//Used to track active alerts
var alerts = {};
exports.alerts = alerts;

//Regions - Used for Territory Control calculations, and Continent locks.
var regions = {};

//Websocket Client. Connects to SOE's Census REST API.
var wsClient = new persistentClient();

function init(callback)
{
	//Region Data
	var worldsProcessed = 0;
	for(var i=0; i<worlds.length; i++)
	{
		var world = worlds[i];
		regions[world] = {};
		
		initRegionData(world, function()
		{
			worldsProcessed++;
			if(worldsProcessed >= worlds.length)
			{
				//Update Alert Data
				var timestamp = Math.round(Date.now() / 1000) - 7201;
				GetCensusData("http://census.soe.com/s:" + serviceID + "/get/ps2:v2/world_event/?type=METAGAME&c:limit=1000&c:lang=en&after=" + timestamp, true, function(success, data)
				{
					if(success)
					{
						var finishedAlerts = [];
						
						for(var i = 0; i < data.world_event_list.length; i++)
						{
							var alert = data.world_event_list[i];
							if(alert.metagame_event_state == "137" || alert.metagame_event_state == "138")
							{
								finishedAlerts.push(alert.instance_id);
							}
						}
						
						pool.getConnection(function(err, dbConnection)
						{
							if(dbConnection != undefined)
							{
								dbConnection.query('DELETE FROM AlertEvents WHERE status = 1', function(err, result)
								{
									if (err)
									{
										console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
										console.log(err);
									}
									
									dbConnection.release();
								});
							}
						});
						
						for(var i = 0; i < data.world_event_list.length; i++)
						{
							var alert = data.world_event_list[i];
							if((alert.metagame_event_state == "135" || alert.metagame_event_state == "136") && finishedAlerts.indexOf(alert.instance_id) < 0)
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
						
						eventServer = require('./eventServer.js');
						callback();
					}
				});
			}
		});
	}
}

function initRegionData(world, callback)
{
	//Region Data
	GetCensusData("http://census.soe.com/s:" + serviceID + "/get/ps2:v2/map?world_id=" + world + "&zone_ids=" + zones.join(",") + "&c:join=map_region^on:Regions.Row.RowData.RegionId^to:map_region_id^inject_at:map_region^show:facility_id'facility_type_id'map_region_id", false, function(success, data)
	{
		if(success)
		{
			for(var j = 0; j < data.map_list.length; j++)
			{
				var zoneData = data.map_list[j];
				var zoneID = zoneData.ZoneId;
				
				regions[world][zoneID] =
				{
					regions: {},
					locked: 'false',
					locked_by: '0'
				}
				
				for(var k = 0; k < zoneData.Regions.Row.length; k++)
				{
					var regionData = zoneData.Regions.Row[k].RowData;
					var facilityID = regionData.map_region.facility_id;
					
					var regionInfo = {
						'facility_id': facilityID,
						'facility_type_id': regionData.map_region.facility_type_id,
						'owner': regionData.FactionId,
						'zone_id': zoneID
					};
					
					regions[world][zoneID]['regions'][facilityID] = regionInfo;
				}
				
				var regionInfo = calculateTerritoryControl(getSelectedRegions(world, zoneID, 0));
				if(regionInfo.controlVS == 100 || regionInfo.controlNC == 100 || regionInfo.controlTR == 100)
				{
					regions[world][zoneID].locked = 'true';
					regions[world][zoneID].locked_by = regionInfo.majorityController;
				}
			}
			
			callback();
		}
		else
		{
			worlds.splice(worlds.indexOf(world), 1);
			console.log("SEVERE: [World Resolve Error] - Could not retrieve region data for world " + world + ". Is the game server offline, or are your game data definitions out of date?");
			callback();
		}
	});
}

/**************
    Client    *
**************/

//Connection Watcher - Reconnects if websocket connection is dropped.
function watcher()
{
	if(!wsClient.isConnected())
	{
		console.log('Reconnecting...');
		wsClient = new persistentClient();
	}
}
setInterval(watcher, 3000);

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
		return client;
	}
	
	client = new WebSocket('wss://push.planetside2.com/streaming?service-id=s:' + serviceID);
		
	//Events
	client.on('open', function()
	{
		console.log((new Date()) + ' WebSocket client connected!');
	});
	client.on('message', function(data, flags)
	{
		processMessage(data);
	});
	client.on('error', function(error)
	{
		console.log((new Date()) + ' Connect Error: ' + error.toString());
		connected = false;
		online = false;
	});
	client.on('close', function(code)
	{
		console.log((new Date()) + ' Websocket Connection Closed [' + code +']');
		connected = false;
		online = false;
	});
}

function GoOnline()
{
	if(!online)
	{
		if(wsClient.GetClient() != undefined)
		{
			init(function()
			{
				var messageToSend = JSON.stringify(
				{
					"service": "event",
					"action": "subscribe",
					"characters": ["all"],
					"worlds": worlds,
					"eventNames": ["Death","FacilityControl","MetagameEvent","PlayerLogin","PlayerLogout","VehicleDestroy", "BattleRankUp"]
				});
				
				wsClient.GetClient().send(messageToSend);
				
				online = true;
			});
		}
	}
}

function GoOffline()
{
	if(online)
	{
		online = false;
		if(wsClient.GetClient() != undefined)
		{
			var messageToSend = '{"action":"clearSubscribe","all":"true","service":"event"}';
			wsClient.GetClient().send(messageToSend);
		}
	}
}

/**********************
    Message Handler   *
***********************/

//Processes Messages received from the client.
function processMessage(messageData)
{
	var message = JSON.parse(messageData);
	
	//Events Generated Locally
	if(message.service == "local" && message.type == "serviceMessage" && message.payload != undefined)
	{
		var payload = message.payload;
		var eventType = payload.event_name;
		
		if(eventType == "DirectiveCompleted")
		{
			var character = payload.character_id;
			
			if(isValidCharacter(character))
			{
				retrieveCharacterInfo([character], function(success)
				{
					if(success)
					{
						var characterOutfitID = characters[character].outfit_id;
						var characterFactionID = characters[character].faction_id;
						
						var post =
						{
							character_id: character,
							outfit_id: characterOutfitID,
							faction_id: characterFactionID,
							directive_tier_id: payload.directive_tier_id,
							directive_tree_id: payload.directive_tree_id,
							timestamp: payload.timestamp,
							world_id: payload.world_id
						};
					
						pool.getConnection(function(err, dbConnection)
						{
							if(dbConnection != undefined)
							{
								dbConnection.query('INSERT INTO DirectiveEvents SET ?', post, function(err, result)
								{
									if (!err) //TODO Very hacky way of preventing duplicates.
									{
										var messageData =
										{
											character_id: character,
											outfit_id: characterOutfitID,
											faction_id: characterFactionID,
											directive_tier_id: payload.directive_tier_id,
											directive_tree_id: payload.directive_tree_id,
											timestamp: payload.timestamp,
											world_id: payload.world_id
										}
										
										var filterData = 
										{
											characters: [character],
											outfits: [characterOutfitID],
											factions: [characterFactionID],
											directive_tiers: [payload.directive_tier_id],
											directive_trees: [payload.directive_tree_id],
											worlds: [payload.world_id]
										}
										
										var eventData =
										{
											eventType: "DirectiveCompleted",
											messageData: messageData,
											filterData: filterData
										};
										
										eventServer.broadcastEvent(eventData);
									}

									dbConnection.release();
								});
							}
						});
					}
				});
			}
		}
		
		else if(eventType == "AchievementEarned")
		{
			var character = payload.character_id;
			
			if(isValidCharacter(character))
			{
				retrieveCharacterInfo([character], function(success)
				{
					if(success)
					{
						var characterOutfitID = characters[character].outfit_id;
						var characterFactionID = characters[character].faction_id;
						
						var post =
						{
							character_id: character,
							outfit_id: characterOutfitID,
							faction_id: characterFactionID,
							achievement_id: payload.achievement_id,
							timestamp: payload.timestamp,
							zone_id: payload.zone_id,
							world_id: payload.world_id
						};
					
						pool.getConnection(function(err, dbConnection)
						{
							if(dbConnection != undefined)
							{
								dbConnection.query('INSERT INTO AchievementEvents SET ?', post, function(err, result)
								{
									if (!err) //TODO Very hacky way of preventing duplicates.
									{
										var messageData =
										{
											character_id: character,
											outfit_id: characterOutfitID,
											faction_id: characterFactionID,
											achievement_id: payload.achievement_id,
											timestamp: payload.timestamp,
											zone_id: payload.zone_id,
											world_id: payload.world_id
										}
										
										var filterData = 
										{
											characters: [character],
											outfits: [characterOutfitID],
											factions: [characterFactionID],
											achievements: [payload.achievement_id],
											zones: [payload.zone_id],
											worlds: [payload.world_id]
										}
										
										var eventData =
										{
											eventType: "AchievementEarned",
											messageData: messageData,
											filterData: filterData
										};
										
										eventServer.broadcastEvent(eventData);
									}
									dbConnection.release();
								});
							}
						});
					}
				});
			}
		}
	}
	
	else
	{
		//Websocket Events provided by Census
		if(message.service == "event" && message.type == "serviceStateChanged" && message.detail == "EventServerEndpoint_1") //TODO make sure this endpoint reflects the correct status of game servers.
		{
			if(message.online == "true")
			{
				GoOnline();
			}
			else if(message.online == "false")
			{
				GoOffline();
			}
		}
		
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
					
					if(payload.attacker_loadout_id != "0")
					{
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
									var characterOutfitID = characters[characterID].outfit_id;
									var characterFactionID = characters[characterID].faction_id;
									var characterName = characters[characterID].character_name;
								
									var attackerOutfitID = characters[attackerCharacterID].outfit_id;
									var attackerFactionID = calculateFactionFromLoadout(payload.attacker_loadout_id);
									var attackerCharacterName = characters[attackerCharacterID].character_name;
									
									var type = "";
									var post = {};
									var messageData = {};
									var filterData = {};
									
									if(eventType == "Death")
									{
										characterFactionID = calculateFactionFromLoadout(payload.character_loadout_id);
										
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
										
										messageData =
										{
											attacker_character_id: attackerCharacterID,
											attacker_character_name: attackerCharacterName,
											attacker_outfit_id: attackerOutfitID,
											attacker_faction_id: attackerFactionID,
											attacker_loadout_id: payload.attacker_loadout_id,
											victim_character_id: characterID,
											victim_character_name: characterName,
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
											if(dbConnection != undefined)
											{
												dbConnection.query('INSERT IGNORE INTO CombatEvents SET ?', post, function(err, result)
												{
													if (err)
													{
														console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
														console.log(err);
													}
													
													dbConnection.release();
												});
											}
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
										
										messageData =
										{
											attacker_character_id: attackerCharacterID,
											attacker_character_name: attackerCharacterName,
											attacker_outfit_id: attackerOutfitID,
											attacker_faction_id: attackerFactionID,
											attacker_vehicle_id: payload.attacker_vehicle_id,
											victim_character_id: characterID,
											victim_character_name: characterName,
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
											if(dbConnection != undefined)
											{
												dbConnection.query('INSERT IGNORE INTO VehicleCombatEvents SET ?', post, function(err, result)
												{
													if (err)
													{
														console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
														console.log(err);
													}
													
													dbConnection.release();
												});
											}
										});
									}
									
									var eventData =
									{
										eventType: type,
										messageData: messageData,
										filterData: filterData
									};
										
									eventServer.broadcastEvent(eventData);
								}
							});
						}
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
						var zoneID = payload.zone_id;
						var facilityID = payload.facility_id;
						
						regions[worldID][zoneID]['regions'][facilityID].owner = payload.new_faction_id;
						
						var selectedRegions = getSelectedRegions(worldID, zoneID, "0");
						
						var controlInfo = calculateTerritoryControl(selectedRegions);
						
						var controlVS = controlInfo.controlVS;
						var controlNC = controlInfo.controlNC;
						var controlTR = controlInfo.controlTR;
						var majorityController = controlInfo.majorityController;
						
						var isCapture = "0";
						if(payload.old_faction_id != payload.new_faction_id)
						{
							isCapture = "1";
						}
						
						var post =
						{
							facility_id: payload.facility_id,
							facility_type_id: regions[worldID][zoneID]['regions'][facilityID].facility_type_id,
							duration_held: payload.duration_held,
							new_faction_id: payload.new_faction_id,
							old_faction_id: payload.old_faction_id,
							is_capture: isCapture,
							timestamp: payload.timestamp,
							zone_id: payload.zone_id,
							control_vs: controlVS,
							control_nc: controlNC,
							control_tr: controlTR,
							majority_controller: majorityController,
							world_id: payload.world_id
						};
						
						pool.getConnection(function(err, dbConnection)
						{
							if(dbConnection != undefined)
							{
								dbConnection.query('INSERT IGNORE INTO FacilityControlEvents SET ?', post, function(err, result)
								{
									if (err)
									{
										console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
										console.log(err);
									}
									
									dbConnection.release();
								});
							}
						});
						
						var messageData =
						{
							facility_id: payload.facility_id,
							facility_type_id: payload.facility_type_id,
							duration_held: payload.duration_held,
							new_faction_id: payload.new_faction_id,
							old_faction_id: payload.old_faction_id,
							is_capture: isCapture,
							timestamp: payload.timestamp,
							zone_id: payload.zone_id,
							control_vs: controlVS,
							control_nc: controlNC,
							control_tr: controlTR,
							majority_controller: majorityController,
							world_id: payload.world_id
						}
						
						var filterData = 
						{
							facilities: [payload.facility_id],
							facility_types: [payload.facility_type_id],
							factions: [payload.new_faction_id, payload.old_faction_id],
							captures: [isCapture],
							zones: [payload.zone_id],
							worlds: [payload.world_id]
						}
						
						var eventData =
						{
							eventType: "FacilityControl",
							messageData: messageData,
							filterData: filterData
						};
					
						eventServer.broadcastEvent(eventData);
						
						//Check if continent is locked.
						if((controlVS == 100 || controlNC == 100 || controlTR == 100) && regions[worldID][zoneID].locked == 'false')
						{
							regions[worldID][zoneID].locked = 'true';
							regions[worldID][zoneID].locked_by = majorityController;
							
							var lockPost =
							{
								zone_id: payload.zone_id,
								world_id: payload.world_id,
								timestamp: payload.timestamp,
								type: '1',
								locked_by: majorityController,
							};
							
							pool.getConnection(function(err, dbConnection)
							{
								if(dbConnection != undefined)
								{
									dbConnection.query('INSERT IGNORE INTO ContinentLockEvents SET ?', lockPost, function(err, result)
									{
										if (err)
										{
											console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
											console.log(err);
										}
										
										dbConnection.release();
									});
								}
							});
							
							var lockMessageData =
							{
								zone_id: payload.zone_id,
								world_id: payload.world_id,
								timestamp: payload.timestamp,
								type: '1',
								type_name: 'locked',
								locked_by: majorityController,
							}
							
							var lockFilterData = 
							{
								zones: [payload.zone_id],
								worlds: [payload.world_id],
								factions: [majorityController],
								types: ['1']
							}
							
							var lockEventData =
							{
								eventType: "ContinentLock",
								messageData: lockMessageData,
								filterData: lockFilterData
							};
						
							eventServer.broadcastEvent(lockEventData);
						}
						else if((controlVS != 100 && controlNC != 100 && controlTR != 100) && regions[worldID][zoneID].locked == 'true')
						{
							regions[worldID][zoneID].locked = 'false';
							regions[worldID][zoneID].locked_by = '0';
							
							var lockPost =
							{
								zone_id: payload.zone_id,
								world_id: payload.world_id,
								timestamp: payload.timestamp,
								type: '0',
								locked_by: '0',
							};
							
							pool.getConnection(function(err, dbConnection)
							{
								if(dbConnection != undefined)
								{
									dbConnection.query('INSERT IGNORE INTO ContinentLockEvents SET ?', lockPost, function(err, result)
									{
										if (err)
										{
											console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
											console.log(err);
										}
										
										dbConnection.release();
									});
								}
							});
							
							var lockMessageData =
							{
								zone_id: payload.zone_id,
								world_id: payload.world_id,
								timestamp: payload.timestamp,
								type: '0',
								type_name: 'unlocked',
								locked_by: '0',
							}
							
							var lockFilterData = 
							{
								zones: [payload.zone_id],
								worlds: [payload.world_id],
								factions: [majorityController],
								types: ['0']
							}
							
							var lockEventData =
							{
								eventType: "ContinentLock",
								messageData: lockMessageData,
								filterData: lockFilterData
							};
						
							eventServer.broadcastEvent(lockEventData);
						}
					}
				}
				
				//Process Event-Specific Data
				else if(eventType == "MetagameEvent")
				{
					if(payload.metagame_event_state == "135" || payload.metagame_event_state == "136")
					{
						retrieveAlertInfo(payload.instance_id, payload.metagame_event_id, payload.world_id, function(uID, controlVS, controlNC, controlTR, majorityController)
						{
							alerts[uID].start_time = payload.timestamp;
							
							var worldID = payload.world_id;
							var zoneID = alerts[uID].zone_id;
							var facilityTypeID = alerts[uID].facility_type_id;
							
							var selectedRegions = getSelectedRegions(worldID, zoneID, facilityTypeID);
							
							var post =
							{
								alert_id: payload.instance_id,
								alert_type_id: payload.metagame_event_id,
								start_time: payload.timestamp,
								end_time: "0",
								status: "1",
								control_vs: controlVS,
								control_nc: controlNC,
								control_tr: controlTR,
								majority_controller: majorityController,
								domination: "0",
								zone_id: zoneID,
								facility_type_id: facilityTypeID,
								world_id: worldID
							};
							
							var messageData =
							{
								alert_id: payload.instance_id,
								alert_type_id: payload.metagame_event_id,
								start_time: payload.timestamp,
								end_time: "0",
								status: "1",
								status_name: "start",
								control_vs: controlVS,
								control_nc: controlNC,
								control_tr: controlTR,
								facilities: selectedRegions,
								majority_controller: majorityController,
								domination: "0",
								zone_id: zoneID,
								facility_type_id: facilityTypeID,
								world_id: worldID
							}
							
							var filterData = 
							{
								alerts: [payload.instance_id],
								alert_types: [payload.metagame_event_id],
								statuses: ["1"],
								dominations: ["0"],
								zones: [zoneID],
								facility_types: [facilityTypeID],
								worlds: [worldID]
							}
							
							pool.getConnection(function(err, dbConnection)
							{
								if(dbConnection != undefined)
								{
									dbConnection.query('INSERT IGNORE INTO AlertEvents SET ?', post, function(err, result)
									{
										if (err)
										{
											console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
											console.log(err);
										}
										
										dbConnection.release();
									});
								}
							});
							
							var eventData =
							{
								eventType: "Alert",
								messageData: messageData,
								filterData: filterData
							};
							
							if(eventServer != undefined)
							{
								eventServer.broadcastEvent(eventData);
							}
						});
					}
					
					else if(payload.metagame_event_state == "137" || payload.metagame_event_state == "138")
					{
						var uID = payload.world_id + "_" + payload.instance_id;
						
						if(alerts[uID] != undefined)
						{
							var startTime = alerts[uID].start_time;
							var zoneID = alerts[uID].zone_id;
							var facilityTypeID = alerts[uID].facility_type_id;
							
							var alertID = payload.instance_id;
							var worldID = payload.world_id;
							
							var selectedRegions = getSelectedRegions(worldID, zoneID, facilityTypeID);
							
							var controlInfo = calculateTerritoryControl(selectedRegions);
							
							var controlVS = controlInfo.controlVS;
							var controlNC = controlInfo.controlNC;
							var controlTR = controlInfo.controlTR;
							var majorityController = controlInfo.majorityController;
							
							var domination = "0";
							if(controlVS >= 94 || controlNC >= 94 || controlTR >= 94)
							{
								domination = "1";
							}
							
							var post =
							{
								end_time: payload.timestamp,
								status: "0",
								control_vs: controlVS,
								control_nc: controlNC,
								control_tr: controlTR,
								majority_controller: majorityController,
								domination: domination
							};
							
							var messageData =
							{
								alert_id: alertID,
								alert_type_id: payload.metagame_event_id,
								start_time: startTime,
								end_time: payload.timestamp,
								status: "0",
								status_name: "end",
								control_vs: controlVS,
								control_nc: controlNC,
								control_tr: controlTR,
								facilities: selectedRegions,
								majority_controller: majorityController,
								domination: domination,
								zone_id: zoneID,
								facility_type_id: facilityTypeID,
								world_id: worldID
							}
							
							var filterData = 
							{
								alerts: [payload.instance_id],
								alert_types: [payload.metagame_event_id],
								statuses: ["0"],
								dominations: [domination],
								zones: [zoneID],
								facility_types: [facilityTypeID],
								worlds: [worldID]
							}
							
							var sql = 'UPDATE AlertEvents SET ? WHERE alert_id = ' + alertID + ' AND world_id = ' + worldID;
							pool.getConnection(function(err, dbConnection)
							{
								if(dbConnection != undefined)
								{
									dbConnection.query(sql, post, function(err, result)
									{
										if (err)
										{
											console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
											console.log(err);
										}
										
										dbConnection.release();
									});
								}
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
						}
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
								if(alerts[alert].world_id == payload.world_id && alerts[alert].zone_id == payload.zone_id)
								{
									uID = alert;
									break;
								}
							}
						}
						
						if(uID != null)
						{
							var zoneID = alerts[uID].zone_id;
							var facilityTypeID = alerts[uID].facility_type_id;
							
							var selectedRegions = getSelectedRegions(payload.world_id, zoneID, facilityTypeID);
							
							var controlInfo = calculateTerritoryControl(selectedRegions);
							
							var controlVS = controlInfo.controlVS;
							var controlNC = controlInfo.controlNC;
							var controlTR = controlInfo.controlTR;
							var majorityController = controlInfo.majorityController;
							
							var post =
							{
								control_vs: controlVS,
								control_nc: controlNC,
								control_tr: controlTR
							};
							
							var alertID = alerts[uID].alert_id;
							var alertTypeID = alerts[uID].alert_type_id;
							var region = regions[payload.world_id][zoneID]['regions'][payload.facility_id];
							var worldID = payload.world_id;
							
							var messageData =
							{
								alert_id: alertID,
								alert_type_id: alertTypeID,
								start_time: alerts[uID].start_time,
								end_time: "0",
								timestamp: payload.timestamp,
								status: "2",
								status_name: "territory_update",
								control_vs: controlVS,
								control_nc: controlNC,
								control_tr: controlTR,
								facility_captured: region,
								domination: "0",
								zone_id: zoneID,
								facility_type_id: facilityTypeID,
								world_id: worldID
							}
							
							var filterData = 
							{
								alerts: [alertID],
								alert_types: [alertTypeID],
								statuses: ["2"],
								dominations: ["0"],
								zones: [zoneID],
								facility_types: [facilityTypeID],
								worlds: [worldID]
							}
							
							var sql = 'UPDATE AlertEvents SET ? WHERE alert_id = ' + alertID + ' AND world_id = ' + payload.world_id;
							pool.getConnection(function(err, dbConnection)
							{
								if(dbConnection != undefined)
								{
									dbConnection.query(sql, post, function(err, result)
									{
										if (err)
										{
											console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
											console.log(err);
										}
										
										dbConnection.release();
									});
								}
							});
							
							var eventData =
							{
								eventType: "Alert",
								messageData: messageData,
								filterData: filterData
							};
							
							eventServer.broadcastEvent(eventData);
						}
					}
				}
			}
			
			else if(eventType == "PlayerLogin" || eventType == "PlayerLogout")
			{
				var isLogin = "0";
				if(eventType == "PlayerLogin")
				{
					isLogin = "1";
				}
				
				var character = payload.character_id;
				
				if(isValidCharacter(character))
				{
					retrieveCharacterInfo([character], function(success)
					{
						if(success)
						{
							var characterOutfitID = characters[character].outfit_id;
							var characterFactionID = characters[character].faction_id;
							
							var post =
							{
								character_id: character,
								outfit_id: characterOutfitID,
								faction_id: characterFactionID,
								is_login: isLogin,
								timestamp: payload.timestamp,
								world_id: payload.world_id
							};
						
							pool.getConnection(function(err, dbConnection)
							{
								if(dbConnection != undefined)
								{
									dbConnection.query('INSERT IGNORE INTO LoginEvents SET ?', post, function(err, result)
									{
										if (err)
										{
											console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
											console.log(err);
										}
										
										dbConnection.release();
									});
								}
							});
	
							var messageData =
							{
								character_id: character,
								outfit_id: characterOutfitID,
								faction_id: characterFactionID,
								is_login: isLogin,
								timestamp: payload.timestamp,
								world_id: payload.world_id
							}
							
							var filterData = 
							{
								characters: [character],
								outfits: [characterOutfitID],
								factions: [characterFactionID],
								login_types: [isLogin],
								worlds: [payload.world_id]
							}
							
							var eventData =
							{
								eventType: "Login",
								messageData: messageData,
								filterData: filterData
							};
							
							eventServer.broadcastEvent(eventData);
						}
					});
				}
			}
			
			else if(eventType == "BattleRankUp")
			{
				var character = payload.character_id;
				
				if(payload.battle_rank > 10)
				{
					if(isValidCharacter(character))
					{
						retrieveCharacterInfo([character], function(success)
						{
							if(success)
							{
								var characterOutfitID = characters[character].outfit_id;
								var characterFactionID = characters[character].faction_id;
								
								var post =
								{
									character_id: character,
									outfit_id: characterOutfitID,
									faction_id: characterFactionID,
									battle_rank: payload.battle_rank,
									timestamp: payload.timestamp,
									zone_id: payload.zone_id,
									world_id: payload.world_id
								};
								
								pool.getConnection(function(err, dbConnection)
								{
									if(dbConnection != undefined)
									{
										dbConnection.query('INSERT IGNORE INTO BattleRankEvents SET ?', post, function(err, result)
										{
											if (err)
											{
												console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
												console.log(err);
											}
											
											dbConnection.release();
										});
									}
								});
	
								var messageData =
								{
									character_id: character,
									outfit_id: characterOutfitID,
									faction_id: characterFactionID,
									battle_rank: payload.battle_rank,
									timestamp: payload.timestamp,
									zone_id: payload.zone_id,
									world_id: payload.world_id
								}
								
								var filterData = 
								{
									characters: [character],
									outfits: [characterOutfitID],
									factions: [characterFactionID],
									battle_ranks: [payload.battle_rank],
									zones: [payload.zone_id],
									worlds: [payload.world_id]
								}
								
								var eventData =
								{
									eventType: "BattleRank",
									messageData: messageData,
									filterData: filterData
								};
								
								eventServer.broadcastEvent(eventData);
							}
						});
					}
				}
			}
		}
		
		else
		{
			console.log((new Date()) + " CENSUS MESSAGE: " + messageData);
		}
	}
}

/****************************
    Non-Websocket Events    *
*****************************/
//Processes live character info
function queryCharacterInfo()
{
	var queries = queriesToProcess.splice(0,150);
	
	if(queries.length > 0)
	{
		var charList = [];
		for(var query in queries)
		{
			if(queries.hasOwnProperty(query))
			{
				for(var i=0; i<queries[query].charList.length; i++)
				{
					if(charList.indexOf(queries[query].charList[i]) == -1)
					{
						charList.push(queries[query].charList[i]);
					}
				}
			}
		}
		
		var url = "http://census.soe.com/s:" + serviceID + "/get/ps2:v2/character?character_id=" + charList.join(",") + "&c:show=character_id,faction_id,name.first" + 
		"&c:join=outfit_member^show:outfit_id^inject_at:outfit";
		GetCensusData(url, false, function(success, data)
		{
			if(success)
			{
				for(var j = 0; j < charList.length; j++)
				{
					characters[charList[j]] =
					{
						outfit_id: "0",
						faction_id: "0",
						character_name: ""
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
							var characterName = character.name.first;
							var characterOutfitID = "0";
							
							if(character.outfit != null)
							{
								characterOutfitID = character.outfit.outfit_id;
							}
						
							characters[charList[j]].outfit_id = characterOutfitID;
							characters[charList[j]].faction_id = characterFactionID;
							characters[charList[j]].character_name = characterName;
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
setInterval(queryCharacterInfo, 1000);

//Processes Character Events that are not available within the websocket API. Events here will hopefully be temporary as websocket events get added to the Census API.
function CensusEvents()
{
	if(online)
	{
		var queryTimestamp = Math.round(Date.now() / 1000) - 1800;
		
		if(queryTimestamp < startTime)
		{
			queryTimestamp = startTime;
		}
		
		var url = "http://census.soe.com/s:" + serviceID + "/get/ps2:v2/characters_directive_tier?completion_time=>" + queryTimestamp + "&c:limit=5000&c:join=characters_world^on:character_id^to:character_id^show:world_id^inject_at:characters_world";
		GetCensusData(url, true, function(success, data)
		{
			if(success)
			{
				var sortedEvents = data.characters_directive_tier_list.sort(compareBy('-completion_time'));
				for(var i=0; i<sortedEvents.length; i++)
				{
					var event = sortedEvents[i];
					var message = 
					{
						'payload':
						{
							"event_name": "DirectiveCompleted",
							character_id: event.character_id,
							timestamp: event.completion_time,
							directive_tier_id: event.directive_tier_id,
							directive_tree_id: event.directive_tree_id,
							world_id: event.characters_world.world_id
						},
						'service': 'local',
						'type':'serviceMessage'
					}
					
					processMessage(JSON.stringify(message));
				}
			}
		});
	}
}
setInterval(CensusEvents, 30000);

//Events that are quickly populated on the REST API can be polled more often.
//TODO: SOE Databases are currently being hammered at the moment due to server merges. Will decrease polling period after performance improves.
var lastEventTimestamp = startTime;
function ActiveCensusEvents()
{
	if(online)
	{
		var url = "http://census.soe.com/s:" + serviceID + "/get/ps2:v2/event?after=>" + lastEventTimestamp + "&type=ACHIEVEMENT&c:limit=5000";
		GetCensusData(url, true, function(success, data)
		{
			if(success)
			{
				var sortedEvents = data.event_list.sort(compareBy('-timestamp'));
				for(var i=0; i<sortedEvents.length; i++)
				{
					var event = sortedEvents[i];
					var message = 
					{
						'payload':
						{
							"event_name": "AchievementEarned",
							character_id: event.character_id,
							timestamp: event.timestamp,
							achievement_id: event.achievement_id,
							zone_id: event.zone_id,
							world_id: event.world_id
						},
						'service': 'local',
						'type':'serviceMessage'
					}
					
					lastEventTimestamp = event.timestamp;
					processMessage(JSON.stringify(message));
				}
			}
		});
	}
}
setInterval(ActiveCensusEvents, 15000);

/************************
    Client Functions    *
************************/

//Checks if the given zone is valid.
function isValidZone(zoneID)
{
	if(zoneID != undefined && zoneID != null)
	{
		if(zoneID < 90)
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

function calculateFactionFromLoadout(loadoutID)
{	
	var factionID = "0";
	
	if(loadoutsVS.indexOf(loadoutID) >= 0)
	{
		factionID = "1";
	}
	
	else if(loadoutsNC.indexOf(loadoutID) >= 0)
	{
		factionID = "2";
	}
	
	else if(loadoutsTR.indexOf(loadoutID) >= 0)
	{
		factionID = "3";
	}
	
	return factionID;
}

//Calculates territory control for the given regions.
function calculateTerritoryControl(selectedRegions)
{
	var totalRegions = 0;
	var facilitiesVS = 0;
	var facilitiesNC = 0;
	var facilitiesTR = 0;
	
	for(var region in selectedRegions)
	{
		totalRegions++;
		if(selectedRegions[region].owner == "1")
		{
			facilitiesVS++;
		}
		else if(selectedRegions[region].owner == "2")
		{
			facilitiesNC++;
		}
		else if(selectedRegions[region].owner == "3")
		{
			facilitiesTR++;
		}
	}
	
	var controlVS = 0;
	var controlNC = 0;
	var controlTR = 0;
	
	if(totalRegions > 0)
	{
		controlVS = Math.floor(facilitiesVS / totalRegions * 100);
		controlNC = Math.floor(facilitiesNC / totalRegions * 100);
		controlTR = Math.floor(facilitiesTR / totalRegions * 100);
	}
	
	var majorityControl = controlVS;
	var majorityController = "1";
	
	if(controlNC > majorityControl)
	{
		majorityControl = controlNC;
		majorityController = "2";
	}
	else if(controlNC == majorityControl)
	{
		majorityController = "0";
	}
	
	if(controlTR > majorityControl)
	{
		majorityControl = controlTR;
		majorityController = "3";
	}
	else if(controlTR == majorityControl)
	{
		majorityController = "0";
	}
	
	var controlInfo = {};
	
	controlInfo.controlVS = controlVS.toString();
	controlInfo.controlNC = controlNC.toString();
	controlInfo.controlTR = controlTR.toString();
	controlInfo.majorityController = majorityController;
	
	return controlInfo;
}

//Gets Regions for a selected alert.
var getActiveAlerts = function(worlds)
{
	var activeAlerts =
	{
		'alerts': {}
	};
	
	for(var alert in alerts)
	{
		var worldID = alerts[alert].world_id;
		
		if(worlds == null || worlds.indexOf(worldID) > -1)
		{
			activeAlerts['alerts'][alert] = alerts[alert];
			
			var alert_type_id = alerts[alert].alert_type_id;
			
			var zoneID = alerts[alert].zone_id;
			var facilityTypeID = alerts[alert].facility_type_id;
			
			var alertRegions = getSelectedRegions(worldID, zoneID, facilityTypeID);
			
			activeAlerts['alerts'][alert]['regions'] = alertRegions;
		}
	}
	
	return activeAlerts;
};
exports.getActiveAlerts = getActiveAlerts;

//Gets the lock status of all continents
var getZoneLockStatus = function(filterWorlds)
{
	var zoneInfo =
	{
		'zoneStatus': {}
	};
	
	for(var world in regions)
	{
		if(filterWorlds == null || filterWorlds.indexOf(world) > -1)
		{
			zoneInfo['zoneStatus'][world] = {};
			for(var zone in regions[world])
			{
				zoneInfo['zoneStatus'][world][zone] =
				{
					locked: regions[world][zone].locked,
					locked_by: regions[world][zone].locked_by
				};
			}
		}
	}
	
	return zoneInfo;
};
exports.getZoneLockStatus = getZoneLockStatus;

//Returns a list of regions based on their world, zone and facility type. Use 0 for facility id if you want to select all non-warpgate regions.
function getSelectedRegions(worldID, zoneID, facilityTypeID)
{
	var selectedRegions = {};
	
	for(var zone in regions[worldID])
	{
		for(var region in regions[worldID][zone]['regions'])
		{
			if(regions[worldID][zone]['regions'][region].facility_type_id != "7")
			{
				if((zone == zoneID && facilityTypeID == "0") || (zoneID == "0" && regions[worldID][zone]['regions'][region].facility_type_id == facilityTypeID) || (zone == zoneID && regions[worldID][zone]['regions'][region].facility_type_id == facilityTypeID))
				{
					selectedRegions[region] = regions[worldID][zone]['regions'][region];
				}
			}
		}
	}
	
	return selectedRegions;
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
			alert_type_id: alertTypeID,
			start_time: "0",
			zone_id: alertTypes[alertTypeID].zone,
			facility_type_id: alertTypes[alertTypeID].facility,
			world_id: worldID,
		}
		
		var zoneID = alertTypes[alertTypeID].zone;
		var facilityTypeID = alertTypes[alertTypeID].facility;
		var selectedRegions = getSelectedRegions(worldID, zoneID, facilityTypeID);
		
		var controlInfo = calculateTerritoryControl(selectedRegions);
		
		var controlVS = controlInfo.controlVS;
		var controlNC = controlInfo.controlNC;
		var controlTR = controlInfo.controlTR;
		var majorityController = controlInfo.majorityController;
		
		callback(uID, controlVS, controlNC, controlTR, majorityController);
	}
}

//A Utility function. Will poll the census API until the requested data is received.
function GetCensusData(url, allowNoData, callback, failureCount)
{
	if(failureCount == undefined)
	{
		failureCount = 0;
	}
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
				console.log("WARNING: [Census Connection Error] - JSON Parse Error (Failed Attempt " + failureCount + ")");
				console.log("Caused by Census Query: " + url);
				if(failureCount >= maxFailures)
				{
					console.log("SEVERE: [Census Connection Error] - Connection Attempt Limit Reached. Dropping event.");
					callback(false, null);
				}
				else
				{
					failureCount++;
					GetCensusData(url, allowNoData, callback, failureCount);
				}
			}
			
			if(data != null && data.returned != undefined && data.returned != null && data.returned != "0")
			{
				callback(true, data);
			}
			
			else if(data != null && data.returned != undefined && data.returned != null && data.returned == "0")
			{
				if(!allowNoData)
				{
					console.log("WARNING: [Census Connection Error] - Expected data was not returned by the query (Failed Attempt " + failureCount + ")");
					console.log("Caused by Census Query: " + url);
					if(failureCount >= maxFailures)
					{
						console.log("SEVERE: [Census Connection Error] - Connection Attempt Limit Reached. Dropping event.");
						callback(false, null);
					}
					else
					{
						failureCount++;
						GetCensusData(url, allowNoData, callback, failureCount);
					}
				}
				else
				{
					callback(true, data);
				}
			}
			
			else
			{
				console.log("WARNING: [Census Connection Error] - An invalid data format was returned (Failure Count " + failureCount + ")");
				console.log("Caused by Census Query: " + url);
				if(failureCount >= maxFailures)
				{
					console.log("SEVERE: [Census Connection Error] - Connection Attempt Limit Reached. Dropping event.");
					callback(false, null);
				}
				else
				{
					failureCount++;
					GetCensusData(url, allowNoData, callback, failureCount);
				}
			}
		});
	}).on('error', function(e)
	{
		if(failureCount >= maxFailures)
		{
			console.log("SEVERE: [Census Connection Error] - Connection Attempt Limit Reached. Dropping event.");
			console.log("Caused by Census Query: " + url);
			callback(false, null);
		}
		else
		{
			console.log("WARNING: [Census Connection Error] - Failed Attempt " + failureCount);
			console.log("Caused by Census Query: " + url);
			failureCount++;
			GetCensusData(url, allowNoData, callback, failureCount);
		}
	});
}

//Array Comparable. Sorts by an object property in an array of objects. Defaults to Descending order, use a - prefix for Ascending order.
function compareBy(property)
{
	var sortOrder = -1;
	if(property[0] === "-") {
		sortOrder = 1;
		property = property.substr(1);
	}
	return function (a,b)
	{
		var result = (a[property] < b[property]) ? -1 : (a[property] > b[property]) ? 1 : 0;
		return result * sortOrder;
	}
}