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

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import cuchaz.ships.EntityShip;

public class PacketShipBlockEvent extends Packet
{
	public static final String Channel = "shipBlockEvent";
	
	private int m_entityId;
	private int m_x;
	private int m_y;
	private int m_z;
	private int m_blockId;
	private int m_eventId;
	private int m_eventParam;
	
	public PacketShipBlockEvent( )
	{
		super( Channel );
	}
	
	public PacketShipBlockEvent( int entityId, int x, int y, int z, int blockId, int eventId, int eventParam )
	{
		this();
		
		m_entityId = entityId;
		m_x = x;
		m_y = y;
		m_z = z;
		m_blockId = blockId;
		m_eventId = eventId;
		m_eventParam = eventParam;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_entityId );
		out.writeInt( m_x );
		out.writeInt( m_y );
		out.writeInt( m_z );
		out.writeInt( m_blockId );
		out.writeInt( m_eventId );
		out.writeInt( m_eventParam );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_entityId = in.readInt();
		m_x = in.readInt();
		m_y = in.readInt();
		m_z = in.readInt();
		m_blockId = in.readInt();
		m_eventId = in.readInt();
		m_eventParam = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// get the ship
		EntityShip ship = (EntityShip)player.worldObj.getEntityByID( m_entityId );
		if( ship == null )
		{
			return;
		}
		
		// deliver the event
		if( ship.getShipWorld().getBlockId( m_x, m_y, m_z ) == m_blockId )
		{
			Block.blocksList[m_blockId].onBlockEventReceived( ship.getShipWorld(), m_x, m_y, m_z, m_eventId, m_eventParam );
		}
	}
}
