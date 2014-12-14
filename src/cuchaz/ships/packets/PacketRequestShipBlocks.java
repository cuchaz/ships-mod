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
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLocator;

public class PacketRequestShipBlocks extends Packet<PacketRequestShipBlocks>
{
	private int m_entityId;
	
	public PacketRequestShipBlocks( )
	{
		// for registration
	}
	
	public PacketRequestShipBlocks( int entityId )
	{
		m_entityId = entityId;
	}
	
	@Override
	public void toBytes( ByteBuf buf )
	{
		buf.writeInt( m_entityId );
	}
	
	@Override
	public void fromBytes( ByteBuf buf )
	{
		m_entityId = buf.readInt();
	}
	
	@Override
	protected IMessage onReceivedServer( NetHandlerPlayServer netServer )
	{
		// get the ship
		EntityShip ship = ShipLocator.getShip( netServer.playerEntity.worldObj, m_entityId );
		if( ship == null )
		{
			return null;
		}
		
		// respond with the blocks
		return new PacketShipBlocks( ship );
	}
}
