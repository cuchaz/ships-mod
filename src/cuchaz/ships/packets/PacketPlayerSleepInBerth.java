/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import cuchaz.ships.EntityShip;
import cuchaz.ships.PlayerRespawner;
import cuchaz.ships.ShipLocator;
import cuchaz.ships.ShipWorld;

public class PacketPlayerSleepInBerth extends Packet
{
	public static final String Channel = "sleepBerth";
	
	private static final int NotAShip = -1;
	
	private int m_playerEntityId;
	private int m_shipEntityId;
	private int m_x;
	private int m_y;
	private int m_z;
	
	protected PacketPlayerSleepInBerth( )
	{
		super( Channel );
	}
	
	public PacketPlayerSleepInBerth( EntityPlayer player, World world, int x, int y, int z )
	{
		this();
		
		m_playerEntityId = player.getEntityId();
		m_shipEntityId = NotAShip;
		m_x = x;
		m_y = y;
		m_z = z;
		
		// is this on a ship?
		if( world instanceof ShipWorld )
		{
			EntityShip ship = ((ShipWorld)world).getShip();
			m_shipEntityId = ship.getEntityId();
		}
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_playerEntityId );
		out.writeInt( m_shipEntityId );
		out.writeInt( m_x );
		out.writeInt( m_y );
		out.writeInt( m_z );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_playerEntityId = in.readInt();
		m_shipEntityId = in.readInt();
		m_x = in.readInt();
		m_y = in.readInt();
		m_z = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// NOTE: the sleeping player isn't necessarily the player that received the packet
		// but they are in the same world
		World world = player.worldObj;
		
		// get the sleeping player
		Entity entity = world.getEntityByID( m_playerEntityId );
		if( entity == null || !( entity instanceof EntityPlayer ) )
		{
			return;
		}
		EntityPlayer sleepingPlayer = (EntityPlayer)entity;
		
		if( m_shipEntityId == NotAShip )
		{
			// sleep in the berth in the world
			PlayerRespawner.sleepInBerthAt( world, m_x, m_y, m_z, sleepingPlayer );
		}
		else
		{
			// get the ship
			EntityShip ship = ShipLocator.getShip( player.worldObj, m_shipEntityId );
			if( ship == null )
			{
				return;
			}
			
			// sleep in the berth on the ship
			PlayerRespawner.sleepInBerthAt( ship.getShipWorld(), m_x, m_y, m_z, sleepingPlayer );
		}
	}
}
