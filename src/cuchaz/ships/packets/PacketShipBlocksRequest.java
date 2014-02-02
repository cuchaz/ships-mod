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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cuchaz.ships.EntityShip;
import cuchaz.ships.Ships;

public class PacketShipBlocksRequest extends Packet
{
	public static final String Channel = "shipBlocksRqst";
	
	private int m_entityId;
	
	public PacketShipBlocksRequest( )
	{
		super( Channel );
	}
	
	public PacketShipBlocksRequest( EntityShip ship )
	{
		this();
		
		m_entityId = ship.entityId;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_entityId );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_entityId = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// get the ship
		Entity entity = player.worldObj.getEntityByID( m_entityId );
		if( entity == null || !( entity instanceof EntityShip ) )
		{
			Ships.logger.warning( "Server dropping PacketShipBlocksRequest from client ship %d! Can't find the ship!", m_entityId );
			return;
		}
		EntityShip ship = (EntityShip)entity;
		
		// send the blocks data
		PacketDispatcher.sendPacketToPlayer( new PacketShipBlocks( ship ).getCustomPacket(), (Player)player );
	}
}
