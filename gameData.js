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

//TODO Automatically resolve data in this file for update proofing.

var data = {}

//Loadout ID's
//Used to resolve faction ID's for Combat Events
data.loadoutsVS = ["15","17","18","19","20","21"];
data.loadoutsNC = ["1","3","4","5","6","7"];
data.loadoutsTR = ["8","10","11","12","13","14"];

//Alert ID's
//Used to resolve Continent and Facility ID's for Alert Events.
data.alertTypes =
{
	"1":{zone:"2", facility:"0"}, //Indar Territory
	"2":{zone:"8", facility:"0"}, //Esamir Territory
	"3":{zone:"6", facility:"0"}, //Amerish Territory
	"4":{zone:"4", facility:"0"}, //Hossin Territory
	//Halloween
	"51":{zone:"2", facility:"0"}, //Pumpkin Hunt 2014 Indar
	"52":{zone:"8", facility:"0"}, //Pumpkin Hunt 2014 Esamir
	"53":{zone:"6", facility:"0"}, //Pumpkin Hunt 2014 Amerish
	"54":{zone:"4", facility:"0"}, //Pumpkin Hunt 2014 Hossin
};

//World IDs
//Used for map queries.
data.worlds = ["1","10","13","17","19","25"];

//Zone IDs
//Used for map queries.
data.zones = ["2","4","6","8"];

module.exports = data;