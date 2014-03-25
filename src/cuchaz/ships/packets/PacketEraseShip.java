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
import cuchaz.ships.items.ItemShipEraser;

public class PacketEraseShip extends Packet
{
	public static final String Channel = "eraseShip";
	
	private int m_x;
	private int m_y;
	private int m_z;
	
	public PacketEraseShip( )
	{
		super( Channel );
	}
	
	public PacketEraseShip( int x, int y, int z )
	{
		this();
		
		m_x = x;
		m_y = y;
		m_z = z;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_x );
		out.writeInt( m_y );
		out.writeInt( m_z );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_x = in.readInt();
		m_y = in.readInt();
		m_z = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		ItemShipEraser.eraseShip( player.worldObj, player, m_x, m_y, m_z );
	}
}
