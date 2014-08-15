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

/*api DB*/

CREATE TABLE `APIKeys` (
  `api_key` char(32) NOT NULL,
  `name` text,
  `enabled` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*ps2_events DB*/
CREATE TABLE `AchievementEvents` (
  `character_id` bigint(20) NOT NULL DEFAULT '0',
  `outfit_id` bigint(20) DEFAULT NULL,
  `faction_id` tinyint(4) DEFAULT NULL,
  `achievement_id` int(11) NOT NULL DEFAULT '0',
  `timestamp` int(11) NOT NULL DEFAULT '0',
  `zone_id` smallint(6) DEFAULT NULL,
  `world_id` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`timestamp`,`character_id`,`achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AlertEvents` (
  `alert_id` int(11) NOT NULL DEFAULT '0',
  `alert_type_id` smallint(6) DEFAULT NULL,
  `start_time` int(11) DEFAULT NULL,
  `end_time` int(11) DEFAULT NULL,
  `status` tinyint(4) DEFAULT NULL,
  `control_vs` tinyint(4) DEFAULT NULL,
  `control_nc` tinyint(4) DEFAULT NULL,
  `control_tr` tinyint(4) DEFAULT NULL,
  `majority_controller` tinyint(4) DEFAULT NULL,
  `domination` tinyint(4) DEFAULT NULL,
  `facility_type_id` tinyint(4) DEFAULT NULL,
  `zone_id` smallint(6) DEFAULT NULL,
  `world_id` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`alert_id`,`world_id`),
  KEY `startTime_index` (`start_time`) USING BTREE,
  KEY `endTime_index` (`end_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `BattleRankEvents` (
  `character_id` bigint(20) NOT NULL DEFAULT '0',
  `outfit_id` bigint(20) DEFAULT NULL,
  `faction_id` tinyint(4) DEFAULT NULL,
  `battle_rank` smallint(6) DEFAULT '0',
  `timestamp` int(11) NOT NULL DEFAULT '0',
  `zone_id` tinyint(4) DEFAULT NULL,
  `world_id` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`timestamp`,`character_id`),
  KEY `outfit_id_index` (`outfit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CombatEvents` (
  `attacker_character_id` bigint(20) NOT NULL DEFAULT '0',
  `attacker_outfit_id` bigint(20) DEFAULT NULL,
  `attacker_faction_id` tinyint(4) DEFAULT NULL,
  `attacker_loadout_id` tinyint(4) DEFAULT NULL,
  `victim_character_id` bigint(20) NOT NULL DEFAULT '0',
  `victim_outfit_id` bigint(20) DEFAULT NULL,
  `victim_faction_id` tinyint(4) DEFAULT NULL,
  `victim_loadout_id` tinyint(4) DEFAULT NULL,
  `timestamp` int(11) NOT NULL DEFAULT '0',
  `weapon_id` int(11) DEFAULT NULL,
  `vehicle_id` mediumint(9) DEFAULT NULL,
  `headshot` tinyint(4) DEFAULT NULL,
  `zone_id` smallint(6) DEFAULT NULL,
  `world_id` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`timestamp`,`attacker_character_id`,`victim_character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `ContinentLockEvents` (
  `zone_id` smallint(6) NOT NULL DEFAULT '0',
  `world_id` tinyint(4) NOT NULL DEFAULT '0',
  `timestamp` int(11) NOT NULL DEFAULT '0',
  `type` tinyint(4) DEFAULT NULL,
  `locked_by` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`timestamp`,`zone_id`,`world_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `DirectiveEvents` (
  `character_id` bigint(20) NOT NULL DEFAULT '0',
  `outfit_id` bigint(20) DEFAULT NULL,
  `faction_id` tinyint(4) DEFAULT NULL,
  `directive_tier_id` tinyint(4) NOT NULL DEFAULT '0',
  `directive_tree_id` mediumint(9) NOT NULL DEFAULT '0',
  `timestamp` int(11) NOT NULL DEFAULT '0',
  `world_id` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`timestamp`,`character_id`,`directive_tree_id`,`directive_tier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FacilityControlEvents` (
  `facility_id` int(11) NOT NULL DEFAULT '0',
  `facility_type_id` tinyint(4) DEFAULT NULL,
  `duration_held` int(11) DEFAULT NULL,
  `new_faction_id` tinyint(4) DEFAULT NULL,
  `old_faction_id` tinyint(4) DEFAULT NULL,
  `is_capture` tinyint(4) DEFAULT NULL,
  `timestamp` int(11) NOT NULL DEFAULT '0',
  `zone_id` smallint(6) DEFAULT NULL,
  `control_vs` tinyint(4) DEFAULT NULL,
  `control_nc` tinyint(4) DEFAULT NULL,
  `control_tr` tinyint(4) DEFAULT NULL,
  `majority_controller` tinyint(4) DEFAULT NULL,
  `world_id` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`timestamp`,`world_id`,`facility_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `LoginEvents` (
  `character_id` bigint(20) NOT NULL DEFAULT '0',
  `outfit_id` bigint(20) DEFAULT NULL,
  `faction_id` tinyint(4) DEFAULT NULL,
  `is_login` tinyint(1) DEFAULT NULL,
  `timestamp` int(11) NOT NULL DEFAULT '0',
  `world_id` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`timestamp`,`character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `VehicleCombatEvents` (
  `attacker_character_id` bigint(20) NOT NULL DEFAULT '0',
  `attacker_outfit_id` bigint(20) DEFAULT NULL,
  `attacker_faction_id` tinyint(4) DEFAULT NULL,
  `attacker_loadout_id` tinyint(4) DEFAULT NULL,
  `attacker_vehicle_id` mediumint(9) DEFAULT NULL,
  `victim_character_id` bigint(20) NOT NULL DEFAULT '0',
  `victim_outfit_id` bigint(20) DEFAULT NULL,
  `victim_faction_id` tinyint(4) DEFAULT NULL,
  `victim_vehicle_id` mediumint(9) DEFAULT NULL,
  `timestamp` int(11) NOT NULL DEFAULT '0',
  `weapon_id` int(11) DEFAULT NULL,
  `zone_id` smallint(6) DEFAULT NULL,
  `world_id` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`timestamp`,`attacker_character_id`,`victim_character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;