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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipWorld;

public class PacketChangedBlocks extends Packet
{
	public static final String Channel = "changedBlocks";
	
	private BlockSet m_changedBlocks;
	private EntityShip m_ship;
	private int m_entityId;
	private int m_numChangedBlocks;
	private int[] m_x;
	private int[] m_y;
	private int[] m_z;
	private int[] m_blockId;
	private int[] m_meta;
	
	public PacketChangedBlocks( )
	{
		super( Channel );
	}
	
	public PacketChangedBlocks( EntityShip ship, BlockSet changedBlocks )
	{
		this();
		
		m_changedBlocks = changedBlocks;
		m_ship = ship;
	}
	
	@Override
	public void writeData( DataOutputStream out ) throws IOException
	{
		ShipWorld world = m_ship.getShipWorld();
		out.writeInt( m_ship.entityId );
		out.writeInt( m_changedBlocks.size() );
		for( ChunkCoordinates coords : m_changedBlocks )
		{
			out.writeShort( coords.posX );
			out.writeShort( coords.posY );
			out.writeShort( coords.posZ );
			out.writeShort( world.getBlockId( coords ) );
			out.writeByte( world.getBlockMetadata( coords ) );
		}
	}
	
	@Override
	public void readData( DataInputStream in ) throws IOException
	{
		// read the header
		m_entityId = in.readInt();
		m_numChangedBlocks = in.readInt();
		
		// allocate space for the rest
		m_x = new int[m_numChangedBlocks];
		m_y = new int[m_numChangedBlocks];
		m_z = new int[m_numChangedBlocks];
		m_blockId = new int[m_numChangedBlocks];
		m_meta = new int[m_numChangedBlocks];
		
		// read the changes into a buffer
		for( int i=0; i<m_numChangedBlocks; i++ )
		{
			m_x[i] = in.readShort();
			m_y[i] = in.readShort();
			m_z[i] = in.readShort();
			m_blockId[i] = in.readShort();
			m_meta[i] = in.readByte();
		}
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
		
		// apply the block changes
		ShipWorld world = ship.getShipWorld();
		for( int i=0; i<m_numChangedBlocks; i++ )
		{
			world.applyBlockChange( m_x[i], m_y[i], m_z[i], m_blockId[i], m_meta[i] );
		}
	}
}
