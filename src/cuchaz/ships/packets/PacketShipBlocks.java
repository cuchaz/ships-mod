/*******************************************************************************
 * Copyright (c) 2014 jeff.
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

import net.minecraft.entity.player.EntityPlayer;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipWorld;

public class PacketShipBlocks extends Packet
{
	public static final String Channel = "shipBlocks";
	
	private int m_entityId;
	private byte[] m_blocksData;
	
	public PacketShipBlocks( )
	{
		super( Channel );
	}
	
	public PacketShipBlocks( EntityShip ship )
	{
		this();
		
		m_entityId = ship.entityId;
		m_blocksData = ship.getShipWorld().getData();
	}

	@Override
	public void writeData( DataOutputStream out ) throws IOException
	{
		out.writeInt( m_entityId );
		out.writeInt( m_blocksData.length );
		out.write( m_blocksData );
	}
	
	@Override
	public void readData( DataInputStream in ) throws IOException
	{
		m_entityId = in.readInt();
		m_blocksData = new byte[in.readInt()]; // this is potentially risky?
		in.read( m_blocksData );
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
		
		// send the blocks to the ship
		if( ship.getShipWorld() == null )
		{
			ship.setShipWorld( new ShipWorld( ship.worldObj, m_blocksData ) );
		}
	}
}
