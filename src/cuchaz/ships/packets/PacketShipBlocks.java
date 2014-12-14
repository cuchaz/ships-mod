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
import cuchaz.ships.ShipLocator;
import cuchaz.ships.Ships;
import cuchaz.ships.persistence.PersistenceException;
import cuchaz.ships.persistence.ShipWorldPersistence;

public class PacketShipBlocks extends Packet
{
	public static final String Channel = "shipBlocks";
	
	private int m_entityId;
	private byte[] m_shipData;
	
	public PacketShipBlocks( )
	{
		super( Channel );
	}
	
	public PacketShipBlocks( EntityShip ship )
	{
		this();
		
		m_entityId = ship.getEntityId();
		m_shipData = ShipWorldPersistence.writeNewestVersion( ship.getShipWorld(), true );
	}

	@Override
	public void writeData( DataOutputStream out ) throws IOException
	{
		out.writeInt( m_entityId );
		out.writeInt( m_shipData.length );
		out.write( m_shipData );
	}
	
	@Override
	public void readData( DataInputStream in ) throws IOException
	{
		m_entityId = in.readInt();
		m_shipData = new byte[Math.min( in.readInt(), MaxPacketSize )];
		in.read( m_shipData );
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// get the ship
		EntityShip ship = ShipLocator.getShip( player.worldObj, m_entityId );
		if( ship == null )
		{
			return;
		}
		
		// send the blocks to the ship
		if( ship.getShipWorld() == null )
		{
			try
			{
				ship.setShipWorld( ShipWorldPersistence.readAnyVersion( ship.worldObj, m_shipData, true ) );
			}
			catch( PersistenceException ex )
			{
				Ships.logger.warning( ex, "Unable to read ship! Ship will be removed from world" );
				ship.setDead();
			}
		}
	}
}
