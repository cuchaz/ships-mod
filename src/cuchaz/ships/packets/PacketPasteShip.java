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
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.ShipClipboard;
import cuchaz.ships.Ships;
import cuchaz.ships.persistence.PersistenceException;

public class PacketPasteShip extends Packet<PacketPasteShip>
{
	private String m_encodedBlocks;
	private int m_dx;
	private int m_dy;
	private int m_dz;
	
	public PacketPasteShip( )
	{
		// for registation
	}
	
	public PacketPasteShip( String encodedBlocks, int dx, int dy, int dz )
	{
		m_encodedBlocks = encodedBlocks;
		m_dx = dx;
		m_dy = dy;
		m_dz = dz;
	}
	
	@Override
	public void toBytes( ByteBuf buf )
	{
		ByteBufUtils.writeUTF8String( buf, m_encodedBlocks );
		buf.writeInt( m_dx );
		buf.writeInt( m_dy );
		buf.writeInt( m_dz );
	}
	
	@Override
	public void fromBytes( ByteBuf buf )
	{
		m_encodedBlocks = ByteBufUtils.readUTF8String( buf );
		m_dx = buf.readInt();
		m_dy = buf.readInt();
		m_dz = buf.readInt();
	}
	
	@Override
	protected IMessage onReceivedServer( NetHandlerPlayServer netServer )
	{
		if( m_encodedBlocks == null )
		{
			return null;
		}
		
		// restore the ship blocks on the server
		try
		{
			ShipClipboard.restoreShip(
				netServer.playerEntity.worldObj,
				m_encodedBlocks,
				new Coords( m_dx, m_dy, m_dz )
			);
		}
		catch( PersistenceException ex )
		{
			Ships.logger.warning( ex, "Unable to restore ship!" );
		}
		
		return null;
	}
}