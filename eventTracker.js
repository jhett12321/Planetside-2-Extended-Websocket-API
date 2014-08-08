//This file processes the raw data provided by SOE's Census REST API, and saves the data to a local database, and a relay Websocket Server.

//TODO:
		//Census Events:
			//Fully Implemented: 						Death
			//Mostly Implemented [No hist. API]: 		VehicleDestroy, MetagameEvent, FacilityControl
			//Client Implemented [No Relay/hist. API]:  PlayerLogin, PlayerLogout, BattleRankUp
			//Not Yet Implemented: 						ItemAdded, SkillAdded

//Includes
var WebSocket = require('ws');
var mysql = require('mysql');
var http = require('http');
var url = require('url');

var eventServer;
var config = require('./config.js');

//Census/Application status. Turns false if census queries fail, or timeout.
var online = false;

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
	//Loadout ID's
	//Used to resolve faction ID's for Combat Events
var loadoutsVS = ["15","17","18","19","20","21"];
var loadoutsNC = ["1","3","4","5","6","7"];
var loadoutsTR = ["8","10","11","12","13","14"];

	//Alert ID's
	//Used to resolve Continent and Facility ID's for Alert Events.
var alertTypes =
{
	"31":{zone:"2", facility:"0"}, //Indar Lock
	"32":{zone:"8", facility:"0"}, //Esamir Lock
	"33":{zone:"6", facility:"0"}, //Amerish Lock
	"34":{zone:"4", facility:"0"}, //Hossin Lock
};

	//World IDs
	//Used for map queries.
var worlds = ["1","10","13","17","19","25"];

	//Zone IDs
	//Used for map queries.
var zones = ["2","4","6","8"];

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
		var world = worlds[i];
		regions[world] = {};
		
		initRegionData(world, function(isLastWorld)
		{
			if(isLastWorld)
			{	
				//Update Alert Data
				var limit = worlds.length * zones.length * 2; //Max possible alert events - 1 start and end event for each continent on each server
				GetCensusData("http://census.soe.com/s:" + serviceID + "/get/ps2:v2/world_event/?type=METAGAME&c:limit=" + limit + "&c:lang=en", function(success, data)
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
									}
									else
									{
										dbConnection.release();
									}
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
	GetCensusData("http://census.soe.com/s:" + serviceID + "/get/ps2:v2/map?world_id=" + world + "&zone_ids=" + zones.join(",") + "&c:join=map_region^on:Regions.Row.RowData.RegionId^to:map_region_id^inject_at:map_region^show:facility_id'facility_type_id'map_region_id", function(success, data)
	{
		if(success)
		{
			for(var j = 0; j < data.map_list.length; j++)
			{
				var zoneData = data.map_list[j];
				var zoneID = zoneData.ZoneId;
				
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
					
					regions[world][facilityID] = regionInfo;
				}
			}
			
			if(world == worlds[worlds.length - 1])
			{
				callback(true);
			}
			else
			{
				callback(false);
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
		console.log('Reconnecting...');
		wsClient = new persistentClient();
	}
}

//Processes live character info
function queryCharacterInfo()
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
		
		var url = "http://census.soe.com/s:" + serviceID + "/get/ps2:v2/character?character_id=" + charList.join(",") + "&c:show=character_id,faction_id,name.first&c:join=outfit_member^on:character_id^to:character_id^show:outfit_id";
		GetCensusData(url, function(success, data)
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
							
							if(character.character_id_join_outfit_member != null)
							{
								characterOutfitID = character.character_id_join_outfit_member.outfit_id;
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
setInterval(watcher, 3000);
setInterval(queryCharacterInfo, 1000);

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
	});
	client.on('close', function(code)
	{
		console.log((new Date()) + ' Websocket Connection Closed [' + code +']');
		connected = false;
	});
	

}

function GoOnline()
{
	if(!online)
	{
		online = true;
		if(wsClient.GetClient() != undefined)
		{
			init(function()
			{
				var messageToSend = '{"service":"event","action":"subscribe","characters":["all"],"worlds":["all"],"eventNames":["Death","FacilityControl","MetagameEvent","PlayerLogin","PlayerLogout","VehicleDestroy", "BattleRankUp"]}';
				wsClient.GetClient().send(messageToSend);
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

/************************
    Client Functions    *
************************/

//Processes Messages received from the client.
function processMessage(messageData)
{
	var message = JSON.parse(messageData);
	
	if(message.service == "event" && message.type == "serviceStateChanged" && message.detail == "EventServerEndpoint_1")
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
												}
												else
												{
													dbConnection.release();
												}
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
												}
												else
												{
													dbConnection.release();
												}
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
					var facilityID = payload.facility_id;
					
					regions[worldID][facilityID].owner = payload.new_faction_id;
					
					var selectedRegions = getSelectedRegions(worldID, payload.zone_id, "0");
					
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
						facility_type_id: regions[worldID][facilityID].facility_type_id,
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
								}
								else
								{
									dbConnection.release();
								}
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
					if(controlVS == 100 || controlNC == 100 || controlTR == 100)
					{
						var lockPost =
						{
							zone_id: payload.zone_id,
							world_id: payload.world_id,
							timestamp: payload.timestamp,
							locked_by: majorityController,
						};
						
						var lockMessageData =
						{
							zone_id: payload.zone_id,
							world_id: payload.world_id,
							timestamp: payload.timestamp,
							locked_by: majorityController,
						}
						
						var lockFilterData = 
						{
							zones: [payload.zone_id],
							worlds: [payload.world_id],
							factions: [majorityController],
						}
						
						var lockEventData =
						{
							eventType: "ContinentLock",
							messageData: lockMessageData,
							filterData: lockFilterData
						};
					
						eventServer.broadcastEvent(lockEventData);
						
						pool.getConnection(function(err, dbConnection)
						{
							if(dbConnection != undefined)
							{
								dbConnection.query('INSERT IGNORE INTO ContinentLockEvents SET ?', lockPost, function(err, result)
								{
									if (err)
									{
										console.log("SEVERE: [MySQL Database Error] - Database Query Failed");
									}
									else
									{
										dbConnection.release();
									}
								});
							}
						});
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
						var zoneID = alertTypes[payload.metagame_event_id].zone;
						var facilityID = alertTypes[payload.metagame_event_id].facility;
						
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
							facility_type_id: facilityID,
							world_id: worldID
						};
						
						var messageData =
						{
							alert_id: payload.instance_id,
							alert_type_id: payload.metagame_event_id,
							start_time: payload.timestamp,
							end_time: "0",
							status: "1",
							control_vs: controlVS,
							control_nc: controlNC,
							control_tr: controlTR,
							facilities: selectedRegions,
							majority_controller: majorityController,
							domination: "0",
							zone_id: alertTypes[payload.metagame_event_id].zone,
							facility_type_id: alertTypes[payload.metagame_event_id].facility,
							world_id: payload.world_id
						}
						
						var filterData = 
						{
							alerts: [payload.instance_id],
							alert_types: [payload.metagame_event_id],
							statuses: ["1"],
							dominations: ["0"],
							zones: [alertTypes[payload.metagame_event_id].zone],
							facilityTypes: [alertTypes[payload.metagame_event_id].facility],
							worlds: [payload.world_id]
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
									}
									else
									{
										dbConnection.release();
									}
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
					var zoneID = alertTypes[payload.metagame_event_id].zone;
					var facilityTypeID = alertTypes[payload.metagame_event_id].facility;
					
					var selectedRegions = getSelectedRegions(payload.world_id, zoneID, facilityTypeID);
					
					var controlInfo = calculateTerritoryControl(selectedRegions);
					
					var controlVS = controlInfo.controlVS;
					var controlNC = controlInfo.controlNC;
					var controlTR = controlInfo.controlTR;
					var majorityController = controlInfo.majorityController;
					
					var uID = payload.world_id + "_" + payload.instance_id;
					
					if(alerts[uID] != undefined)
					{
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
							alert_id: payload.instance_id,
							alert_type_id: payload.metagame_event_id,
							start_time: alerts[uID].start_time,
							end_time: payload.timestamp,
							status: "0",
							control_vs: controlVS,
							control_nc: controlNC,
							control_tr: controlTR,
							facilities: selectedRegions,
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
							statuses: ["0"],
							dominations: [domination],
							zones: [alertTypes[payload.metagame_event_id].zone],
							facilityTypes: [alertTypes[payload.metagame_event_id].facility],
							worlds: [payload.world_id]
						}
						
						var alertID = payload.instance_id;
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
									}
									else
									{
										dbConnection.release();
									}
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
						var region = regions[payload.world_id][payload.facility_id];
						
						var messageData =
						{
							alert_id: alertID,
							alert_type_id: alertTypeID,
							start_time: alerts[uID].start_time,
							end_time: "0",
							timestamp: payload.timestamp,
							status: "2",
							control_vs: controlVS,
							control_nc: controlNC,
							control_tr: controlTR,
							facility_captured: region,
							domination: "0",
							zone_id: alertTypes[alertTypeID].zone,
							facility_type_id: alertTypes[alertTypeID].facility,
							world_id: payload.world_id
						}
						
						var filterData = 
						{
							alerts: [alertID],
							alert_types: [alertTypeID],
							statuses: ["2"],
							dominations: ["0"],
							zones: [alertTypes[alertTypeID].zone],
							facilityTypes: [alertTypes[alertTypeID].facility],
							worlds: [payload.world_id]
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
									}
									else
									{
										dbConnection.release();
									}
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
									}
									else
									{
										dbConnection.release();
									}
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
										}
										else
										{
											dbConnection.release();
										}
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
var getActiveAlerts = function()
{
	var activeAlerts =
	{
		'alerts': {}
	};
	
	for(var alert in alerts)
	{
		if(alert in alerts)
		{
			activeAlerts['alerts'][alert] = alerts[alert];
			
			var worldID = alerts[alert].world_id;
			var alert_type_id = alerts[alert].alert_type_id;
			
			var zoneID = alertTypes[alert_type_id].zone;
			var facilityTypeID = alertTypes[alert_type_id].facility;
			
			var alertRegions = getSelectedRegions(worldID, zoneID, facilityTypeID);
			
			activeAlerts['alerts'][alert]['regions'] = alertRegions;
		}
	}
	
	return activeAlerts;
};
exports.getActiveAlerts = getActiveAlerts;

//Returns a list of regions based on their world, zone and facility type. Use 0 for facility id if you want to select all non-warpgate regions.
function getSelectedRegions(worldID, zoneID, facilityTypeID)
{
	var selectedRegions = {};
	
	for(var region in regions[worldID])
	{
		if(regions[worldID][region].facility_type_id != "7")
		{
			if((regions[worldID][region].zone_id == zoneID && facilityTypeID == "0") || (zoneID == "0" && regions[worldID][region].facility_type_id == facilityTypeID) || (regions[worldID][region].zone_id == zoneID && regions[worldID][region].facility_type_id == facilityTypeID))
			{
				selectedRegions[region] = regions[worldID][region];
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
			world_id: worldID,
			start_time: "0",
			alert_type_id: alertTypeID
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
					console.log("SEVERE: [Census Connection Error] - Connection Attempt Limit Reached. Dropping event.");
					console.log("Caused by Census Query: " + url);
					callback(false, null);
				}
				else
				{
					console.log("WARNING: [Census Connection Error] - Failed Attempt " + failureCount);
					failureCount++;
					GetCensusData(url, callback);
				}
			}
			
			if(data != null && data.returned != undefined && data.returned != null && data.returned != "0")
			{
				callback(true, data);
			}
			
			else
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
					failureCount++;
					GetCensusData(url, callback);
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
			failureCount++;
			GetCensusData(url, callback);
		}
	});
}