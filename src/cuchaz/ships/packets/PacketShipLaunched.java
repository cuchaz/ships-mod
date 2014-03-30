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
import java.util.TreeMap;

import net.minecraft.entity.player.EntityPlayer;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;
import cuchaz.ships.persistence.ShipWorldPersistence;
import cuchaz.ships.persistence.UnrecognizedPersistenceVersion;

public class PacketShipLaunched extends Packet
{
	public static final String Channel = "shipLaunched";
	
	private int m_entityId;
	private byte[] m_blocksData;
	private int m_launchX;
	private int m_launchY;
	private int m_launchZ;
	
	private static TreeMap<Integer,PacketShipLaunched> m_packets;
	
	static
	{
		m_packets = new TreeMap<Integer,PacketShipLaunched>();
	}
	
	public PacketShipLaunched( )
	{
		super( Channel );
	}
	
	public PacketShipLaunched( EntityShip ship, Coords shipBlock )
	{
		this();
		
		m_entityId = ship.entityId;
		m_blocksData = ShipWorldPersistence.writeNewestVersion( ship.getShipWorld() );
		m_launchX = shipBlock.x;
		m_launchY = shipBlock.y;
		m_launchZ = shipBlock.z;
	}

	@Override
	public void writeData( DataOutputStream out ) throws IOException
	{
		out.writeInt( m_entityId );
		out.writeInt( m_blocksData.length );
		out.write( m_blocksData );
		out.writeInt( m_launchX );
		out.writeInt( m_launchY );
		out.writeInt( m_launchZ );
	}

	@Override
	public void readData( DataInputStream in ) throws IOException
	{
		m_entityId = in.readInt();
		m_blocksData = new byte[in.readInt()]; // this is potentially risky?
		in.read( m_blocksData );
		m_launchX = in.readInt();
		m_launchY = in.readInt();
		m_launchZ = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// save the packet for later
		m_packets.put( m_entityId, this );
	}
	
	public static PacketShipLaunched getPacket( EntityShip ship )
	{
		PacketShipLaunched packet = m_packets.get( ship.entityId );
		if( packet != null )
		{
			m_packets.remove( ship.entityId );
		}
		return packet;
	}
	
	public void process( EntityShip ship )
	{
		try
		{
			ShipWorld shipWorld = ShipWorldPersistence.readAnyVersion( ship.worldObj, m_blocksData );
			ShipLauncher.initShip( ship, shipWorld, new Coords( m_launchX, m_launchY, m_launchZ ) );
		}
		catch( UnrecognizedPersistenceVersion ex )
		{
			Ships.logger.warning( ex, "Unable to read ship! Ship will be removed from world" );
			ship.setDead();
		}
	}
}
