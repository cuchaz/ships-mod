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
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cuchaz.ships.EntityShip;

public class PacketRequestShipBlocks extends Packet
{
	public static final String Channel = "requestBlocks";
	
	private int m_entityId;
	
	public PacketRequestShipBlocks( )
	{
		super( Channel );
	}
	
	public PacketRequestShipBlocks( int entityId )
	{
		this();
		
		m_entityId = entityId;
	}
	
	@Override
	public void writeData( DataOutputStream out ) throws IOException
	{
		out.writeInt( m_entityId );
	}
	
	@Override
	public void readData( DataInputStream in ) throws IOException
	{
		m_entityId = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// get the ship
		EntityShip ship = (EntityShip)player.worldObj.getEntityByID( m_entityId );
		if( ship == null )
		{
			return;
		}
		
		// respond with the blocks
		PacketDispatcher.sendPacketToPlayer( new PacketShipBlocks( ship ).getCustomPacket(), (Player)player );
	}
}
