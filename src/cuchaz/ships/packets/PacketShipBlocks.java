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

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.BlockUtils;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;

public class PacketShipBlocks extends Packet
{
	public static final String Channel = "shipBlocks";
	
	private int m_entityId;
	private byte[] m_blocksData;
	
	public PacketShipBlocks( )
	{
		super( Channel );
	}
	
	public PacketShipBlocks( EntityShip ship )
	{
		this();
		
		m_entityId = ship.entityId;
		m_blocksData = ship.getShipWorld().getData();
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_entityId );
		out.writeInt( m_blocksData.length );
		out.write( m_blocksData );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_entityId = in.readInt();
		m_blocksData = new byte[in.readInt()]; // this is potentially risky?
		in.read( m_blocksData );
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// get the ship
		Entity entity = player.worldObj.getEntityByID( m_entityId );
		if( entity == null || !( entity instanceof EntityShip ) )
		{
			Ships.logger.warning( String.format( "Client dropping PacketShipBlocks for client ship %d! Can't find the ship!", m_entityId ) );
			return;
		}
		EntityShip ship = (EntityShip)entity;
		
		// send the block data
		ship.setShipWorld( new ShipWorld( player.worldObj, m_blocksData ) );
		
		// compute the transformation from ship coords to world coords
		Vec3 origin = Vec3.createVectorHelper( 0, 0, 0 );
		ship.blocksToShip( origin );
		ship.shipToWorld( origin );
		int tx = MathHelper.floor_double( origin.xCoord + 0.5 );
		int ty = MathHelper.floor_double( origin.yCoord + 0.5 );
		int tz = MathHelper.floor_double( origin.zCoord + 0.5 );
		
		// remove all the ship blocks from the world, but don't notify the server
		for( ChunkCoordinates coords : ship.getShipWorld().coords() )
		{
			BlockUtils.removeBlockWithoutNotifyingIt( player.worldObj, coords.posX + tx, coords.posY + ty, coords.posZ + tz, false );
			if( coords.posY + ty < ship.getWaterHeight() )
			{
				player.worldObj.setBlock( coords.posX + tx, coords.posY + ty, coords.posZ + tz, Block.waterStill.blockID, 0, 1 );
			}
		}
	}
}
