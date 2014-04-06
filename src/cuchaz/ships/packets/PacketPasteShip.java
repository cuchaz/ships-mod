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
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.entity.player.EntityPlayer;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.BlocksStorage;
import cuchaz.ships.Ships;
import cuchaz.ships.persistence.BlockStoragePersistence;
import cuchaz.ships.persistence.UnrecognizedPersistenceVersion;

public class PacketPasteShip extends Packet
{
	public static final String Channel = "pasteShip";
	
	private BlocksStorage m_blocks;
	private int m_dx;
	private int m_dy;
	private int m_dz;
	
	public PacketPasteShip( )
	{
		super( Channel );
	}
	
	public PacketPasteShip( BlocksStorage blocks, int dx, int dy, int dz )
	{
		this();
		
		m_blocks = blocks;
		m_dx = dx;
		m_dy = dy;
		m_dz = dz;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_dx );
		out.writeInt( m_dy );
		out.writeInt( m_dz );
		GZIPOutputStream gzipOut = new GZIPOutputStream( out );
		BlockStoragePersistence.writeNewestVersion( m_blocks, gzipOut );
		gzipOut.finish();
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_dx = in.readInt();
		m_dy = in.readInt();
		m_dz = in.readInt();
		try
		{
			GZIPInputStream gzipIn = new GZIPInputStream( in );
			m_blocks = BlockStoragePersistence.readAnyVersion( gzipIn );
		}
		catch( UnrecognizedPersistenceVersion ex )
		{
			Ships.logger.warning( "Unable to read ship data", ex );
		}
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		if( m_blocks == null )
		{
			return;
		}
		
		// paste the blocks on the server
		Map<Coords,Coords> correspondence = new TreeMap<Coords,Coords>();
		for( Coords shipCoords : m_blocks.coords() )
		{
			Coords worldCoords = new Coords(
				shipCoords.x + m_dx,
				shipCoords.y + m_dy,
				shipCoords.z + m_dz
			);
			correspondence.put( shipCoords, worldCoords );
		}
		
		m_blocks.writeToWorld( player.worldObj, correspondence );
	}
}