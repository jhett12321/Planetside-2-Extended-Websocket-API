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

var config = {}

config.soeServiceID = "example";

config.dbConnectionLimit = 100;
config.dbHost = '127.0.0.1';
config.dbUser = '';
config.dbPassword = '';
config.dbName = 'ps2_events';

config.apiDbConnectionLimit = 10;
config.apiDbHost = '127.0.0.1';
config.apiDbUser = '';
config.apiDbPassword = '';
config.apiDbName = 'api';

module.exports = config;