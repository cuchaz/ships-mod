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

import cuchaz.ships.config.BlockProperties;
import net.minecraft.entity.player.EntityPlayer;

public class PacketBlockPropertiesOverrides extends Packet
{
	public static final String Channel = "propOverrides";
	
	private String m_overrides;
	
	public PacketBlockPropertiesOverrides( )
	{
		super( Channel );
	}
	
	public PacketBlockPropertiesOverrides( String overrides )
	{
		this();
		
		m_overrides = overrides;
	}

	@Override
	public void writeData( DataOutputStream out ) throws IOException
	{
		out.writeUTF( m_overrides );
	}

	@Override
	public void readData( DataInputStream in ) throws IOException
	{
		m_overrides = in.readUTF();
	}

	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// received on the client
		// save the block properties overrides
		BlockProperties.setOverrides( m_overrides );
	}
}
