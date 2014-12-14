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
import net.minecraft.network.NetHandlerPlayServer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cuchaz.ships.items.ItemShipEraser;

public class PacketEraseShip extends Packet<PacketEraseShip>
{
	private int m_x;
	private int m_y;
	private int m_z;
	
	public PacketEraseShip( )
	{
		// for registration
	}
	
	public PacketEraseShip( int x, int y, int z )
	{
		m_x = x;
		m_y = y;
		m_z = z;
	}
	
	@Override
	public void toBytes( ByteBuf buf )
	{
		buf.writeInt( m_x );
		buf.writeInt( m_y );
		buf.writeInt( m_z );
	}
	
	@Override
	public void fromBytes( ByteBuf buf )
	{
		m_x = buf.readInt();
		m_y = buf.readInt();
		m_z = buf.readInt();
	}
	
	@Override
	protected IMessage onReceivedServer( NetHandlerPlayServer netServer )
	{
		ItemShipEraser.eraseShip( netServer.playerEntity.worldObj, netServer.playerEntity, m_x, m_y, m_z );
		
		return null;
	}
}
