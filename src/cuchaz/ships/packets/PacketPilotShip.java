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

import cpw.mods.fml.common.network.PacketDispatcher;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.Environment;
import cuchaz.ships.EntityShip;

public class PacketPilotShip extends Packet
{
	public static final String Channel = "pilotShip";
	
	private int m_entityId;
	private int m_actions;
	private BlockSide m_sideShipForward;
	private int m_linearThrottle;
	private int m_angularThrottle;
	
	public PacketPilotShip( )
	{
		super( Channel );
	}
	
	public PacketPilotShip( int entityId, int actions, BlockSide sideFacingPlayer, int linearThrottle, int angularThrottle )
	{
		this();
		
		m_entityId = entityId;
		m_actions = actions;
		m_sideShipForward = sideFacingPlayer;
		m_linearThrottle = linearThrottle;
		m_angularThrottle = angularThrottle;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_entityId );
		out.writeInt( m_actions );
		out.writeByte( m_sideShipForward.ordinal() );
		out.writeByte( m_linearThrottle );
		out.writeByte( m_angularThrottle );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_entityId = in.readInt();
		m_actions = in.readInt();
		m_sideShipForward = BlockSide.values()[in.readByte()];
		m_linearThrottle = in.readByte();
		m_angularThrottle = in.readByte();
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
		
		// handle ship movement
		ship.setPilotActions( m_actions, m_sideShipForward, false );
		ship.linearThrottle = m_linearThrottle;
		ship.angularThrottle = m_angularThrottle;
		
		if( Environment.isServer() )
		{
			// broadcast the actions to the rest of the clients
			final double BroadcastRange = 100;
			PacketDispatcher.sendPacketToAllAround( ship.posX, ship.posY, ship.posZ, BroadcastRange, player.worldObj.provider.dimensionId, getCustomPacket() );
		}
	}
}
