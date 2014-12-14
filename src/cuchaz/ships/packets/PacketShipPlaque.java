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

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetHandlerPlayServer;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cuchaz.ships.EntityShipPlaque;
import cuchaz.ships.items.ItemShipPlaque;

public class PacketShipPlaque extends Packet<PacketShipPlaque>
{
	private int m_entityId;
	private String m_name;
	
	public PacketShipPlaque( )
	{
		// for registration
	}
	
	public PacketShipPlaque( EntityShipPlaque shipPlaque )
	{
		m_entityId = shipPlaque.getEntityId();
		m_name = shipPlaque.getName();
	}
	
	@Override
	public void toBytes( ByteBuf buf )
	{
		buf.writeInt( m_entityId );
		ByteBufUtils.writeUTF8String( buf, m_name );
	}
	
	@Override
	public void fromBytes( ByteBuf buf )
	{
		m_entityId = buf.readInt();
		m_name = ByteBufUtils.readUTF8String( buf );
	}
	
	@Override
	protected IMessage onReceivedServer( NetHandlerPlayServer netServer )
	{
		// can this player use the ship plaque?
		if( !ItemShipPlaque.canUse( netServer.playerEntity ) )
		{
			return null;
		}
		
		// get the ship plaque
		Entity entity = netServer.playerEntity.worldObj.getEntityByID( m_entityId );
		if( entity != null && entity instanceof EntityShipPlaque ) 
		{
			EntityShipPlaque shipPlaque = (EntityShipPlaque)entity;
			shipPlaque.setName( m_name );
		}
		
		return null;
	}
}
