package com.blackfeatherproductions.event_tracker.data_dynamic;

import java.util.Date;

import com.blackfeatherproductions.event_tracker.data_static.Faction;
import com.blackfeatherproductions.event_tracker.data_static.World;
import com.blackfeatherproductions.event_tracker.data_static.Zone;

public class OnlinePlayer
{
	private Faction faction;
	private String outfitID; 
	private Zone zone;
	private World world;
	
	private Date lastEvent;
	
	public OnlinePlayer(Faction faction, String outfitID, Zone zone, World world)
	{
		this.faction = faction;
		this.outfitID = outfitID;
		this.zone = zone;
		this.world = world;
		
		this.lastEvent = new Date();
	}
	
	public Zone getZone()
	{
		return zone;
	}
	
	public World getWorld()
	{
		return world;
	}

	public void setWorld(World world)
	{
		this.world = world;
	}

	public Faction getFaction()
	{
		return faction;
	}
	
	public String getOutfitID()
	{
		return outfitID;
	}

	public Date getLastEvent()
	{
		return lastEvent;
	}

	public void setLastEvent(Date lastEvent)
	{
		this.lastEvent = lastEvent;
	}

	public void setZone(Zone zone)
	{
		this.zone = zone;
	}

	public void setFaction(Faction faction)
	{
		this.faction = faction;
	}

	public void setOutfitID(String outfitID)
	{
		this.outfitID = outfitID;
	}
}
