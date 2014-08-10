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
	"31":{zone:"2", facility:"0"}, //Indar Lock
	"32":{zone:"8", facility:"0"}, //Esamir Lock
	"33":{zone:"6", facility:"0"}, //Amerish Lock
	"34":{zone:"4", facility:"0"}, //Hossin Lock
};

//World IDs
//Used for map queries.
data.worlds = ["1","10","13","17","19","25"];

//Zone IDs
//Used for map queries.
data.zones = ["2","4","6","8"];

module.exports = data;