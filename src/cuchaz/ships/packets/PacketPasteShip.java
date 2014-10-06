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
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.Ships;
import cuchaz.ships.items.ItemShipClipboard;
import cuchaz.ships.persistence.PersistenceException;

public class PacketPasteShip extends Packet
{
	public static final String Channel = "pasteShip";
	
	private String m_encodedBlocks;
	private int m_dx;
	private int m_dy;
	private int m_dz;
	
	public PacketPasteShip( )
	{
		super( Channel );
	}
	
	public PacketPasteShip( String encodedBlocks, int dx, int dy, int dz )
	{
		this();
		
		m_encodedBlocks = encodedBlocks;
		m_dx = dx;
		m_dy = dy;
		m_dz = dz;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeUTF( m_encodedBlocks );
		out.writeInt( m_dx );
		out.writeInt( m_dy );
		out.writeInt( m_dz );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_encodedBlocks = in.readUTF();
		m_dx = in.readInt();
		m_dy = in.readInt();
		m_dz = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		if( m_encodedBlocks == null )
		{
			return;
		}
		
		// restore the ship blocks on the server
		try
		{
			ItemShipClipboard.restoreShip( player.worldObj, m_encodedBlocks, new Coords( m_dx, m_dy, m_dz ) );
		}
		catch( PersistenceException ex )
		{
			Ships.logger.warning( ex, "Unable to restore ship!" );
		}
	}
}