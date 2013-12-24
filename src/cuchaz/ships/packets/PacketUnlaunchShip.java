/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipUnlauncher;

public class PacketUnlaunchShip extends Packet
{
	public static final String Channel = "unlaunchShip";
	
	private int m_entityId;
	
	public PacketUnlaunchShip( )
	{
		super( Channel );
	}
	
	public PacketUnlaunchShip( int entityId )
	{
		this();
		
		m_entityId = entityId;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_entityId );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_entityId = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// get the ship
		Entity entity = player.worldObj.getEntityByID( m_entityId );
		if( entity == null || !( entity instanceof EntityShip ) )
		{
			return;
		}
		EntityShip ship = (EntityShip)entity;
		
		// unlaunch the ship
		ShipUnlauncher unlauncher = new ShipUnlauncher( ship );
		if( unlauncher.isUnlaunchable() )
		{
			unlauncher.unlaunch();
		}
	}
}
