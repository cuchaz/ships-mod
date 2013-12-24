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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.Packet250CustomPayload;

public abstract class Packet
{
	private String m_channel;
	
	protected Packet( String channel )
	{
		m_channel = channel;
	}
	
	public Packet250CustomPayload getCustomPacket( )
	{
		Packet250CustomPayload customPacket = new Packet250CustomPayload();
		customPacket.channel = m_channel;
		
		// get the payload
		try
		{
			ByteArrayOutputStream data = new ByteArrayOutputStream( 8 );
			DataOutputStream out = new DataOutputStream( data );
			writeData( out );
			customPacket.data = data.toByteArray();
		}
		catch( IOException ex )
		{
			throw new Error( "Unable to get packet data!", ex );
		}
		customPacket.length = customPacket.data.length;
		
		return customPacket;
	}
	
	public void readCustomPacket( Packet250CustomPayload customPacket )
	{
		try
		{
			DataInputStream in = new DataInputStream( new ByteArrayInputStream( customPacket.data ) );
			readData( in );
			in.close();
		}
		catch( IOException ex )
		{
			throw new Error( "Unable to read data from packet!", ex );
		}
	}
	
	public abstract void writeData( DataOutputStream out ) throws IOException;
	public abstract void readData( DataInputStream in ) throws IOException;
	public abstract void onPacketReceived( EntityPlayer player );
}
