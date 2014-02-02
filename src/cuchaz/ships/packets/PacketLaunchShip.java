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

import net.minecraft.entity.player.EntityPlayer;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.Ships;
import cuchaz.ships.ShipLauncher.LaunchFlag;

public class PacketLaunchShip extends Packet
{
	public static final String Channel = "launchShip";
	
	private int m_x;
	private int m_y;
	private int m_z;
	
	public PacketLaunchShip( )
	{
		super( Channel );
	}
	
	public PacketLaunchShip( int x, int y, int z )
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
		// spawn the ship
		ShipLauncher launcher = new ShipLauncher( player.worldObj, m_x, m_y, m_z );
		if( launcher.isLaunchable() )
		{
			launcher.launch();
		}
		else
		{
			// debug info
			Ships.logger.warning(
				"Server can't launch ship at: (%d,%d,%d)",
				m_x, m_y, m_z
			);
			for( LaunchFlag flag : LaunchFlag.values() )
			{
				Ships.logger.warning( "\t" + flag.name() + ": " + launcher.getLaunchFlag( flag ) );
			}
		}
	}
}
