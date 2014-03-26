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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cuchaz.ships.EntityShipPlaque;
import cuchaz.ships.items.ItemShipPlaque;

public class PacketShipPlaque extends Packet
{
	public static final String Channel = "shipPlaque";
	
	private int m_entityId;
	private String m_name;
	
	public PacketShipPlaque( )
	{
		super( Channel );
	}
	
	public PacketShipPlaque( EntityShipPlaque shipPlaque )
	{
		this();
		
		m_entityId = shipPlaque.entityId;
		m_name = shipPlaque.getName();
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_entityId );
		out.writeUTF( m_name );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_entityId = in.readInt();
		m_name = in.readUTF();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// can this player use the ship plaque?
		if( !ItemShipPlaque.canUse( player ) )
		{
			return;
		}
		
		// get the ship plaque
		Entity entity = player.worldObj.getEntityByID( m_entityId );
		if( entity != null && entity instanceof EntityShipPlaque ) 
		{
			EntityShipPlaque shipPlaque = (EntityShipPlaque)entity;
			shipPlaque.setName( m_name );
		}
	}
}
