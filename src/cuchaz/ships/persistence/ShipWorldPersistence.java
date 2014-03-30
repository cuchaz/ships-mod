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
package cuchaz.ships.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TreeMap;

import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cuchaz.modsShared.blocks.BlockMap;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.BlocksStorage;
import cuchaz.ships.ShipWorld;

public enum ShipWorldPersistence
{
	V1( 1 )
	{
		@Override
		public ShipWorld onRead( World world, DataInputStream in )
		throws IOException
		{
			// read the blocks
			BlocksStorage storage = new BlocksStorage();
			storage.readFromStream( in );
			
			// read the tile entities
			BlockMap<TileEntity> tileEntities = new BlockMap<TileEntity>();
			int numTileEntities = in.readInt();
			for( int i = 0; i < numTileEntities; i++ )
			{
				// create the tile entity
				NBTTagCompound nbt = (NBTTagCompound)NBTBase.readNamedTag( in );
				TileEntity tileEntity = TileEntity.createAndLoadEntity( nbt );
				Coords coords = new Coords( tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord );
				tileEntities.put( coords, tileEntity );
			}
			
			return new ShipWorld( world, storage, tileEntities, new BlockMap<EntityHanging>() );
		}
		
		@Override
		public void onWrite( ShipWorld shipWorld, DataOutputStream out )
		throws IOException
		{
			// write out the blocks
			shipWorld.getBlocksStorage().writeToStream( out );
			
			// write out the tile entities
			out.writeInt( shipWorld.tileEntities().size() );
			for( TileEntity tileEntity : shipWorld.tileEntities().values() )
			{
				NBTTagCompound nbt = new NBTTagCompound();
				tileEntity.writeToNBT( nbt );
				NBTBase.writeNamedTag( nbt, out );
			}
		}
	},
	V2( 2 )
	{
		@Override
		public ShipWorld onRead( World world, DataInputStream in )
		throws IOException
		{
			// read the blocks
			BlocksStorage storage = new BlocksStorage();
			storage.readFromStream( in );
			
			// read the tile entities
			BlockMap<TileEntity> tileEntities = new BlockMap<TileEntity>();
			int numTileEntities = in.readInt();
			for( int i = 0; i < numTileEntities; i++ )
			{
				// create the tile entity
				NBTTagCompound nbt = (NBTTagCompound)NBTBase.readNamedTag( in );
				TileEntity tileEntity = TileEntity.createAndLoadEntity( nbt );
				Coords coords = new Coords( tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord );
				tileEntities.put( coords, tileEntity );
			}
			
			// read the hanging entities
			BlockMap<EntityHanging> hangingEntities = new BlockMap<EntityHanging>();
			int numHangingEntities = in.readInt();
			for( int i = 0; i < numHangingEntities; i++ )
			{
				// create the hanging entity
				NBTTagCompound nbt = (NBTTagCompound)NBTBase.readNamedTag( in );
				EntityHanging hangingEntity = (EntityHanging)EntityList.createEntityFromNBT( nbt, world );
				Coords coords = new Coords( hangingEntity.xPosition, hangingEntity.yPosition, hangingEntity.zPosition );
				hangingEntities.put( coords, hangingEntity );
			}
			
			return new ShipWorld( world, storage, tileEntities, hangingEntities );
		}
		
		@Override
		public void onWrite( ShipWorld shipWorld, DataOutputStream out )
		throws IOException
		{
			// write out the blocks
			shipWorld.getBlocksStorage().writeToStream( out );
			
			// write out the tile entities
			out.writeInt( shipWorld.tileEntities().size() );
			for( TileEntity tileEntity : shipWorld.tileEntities().values() )
			{
				NBTTagCompound nbt = new NBTTagCompound();
				tileEntity.writeToNBT( nbt );
				NBTBase.writeNamedTag( nbt, out );
			}
			
			// write out the tile entities
			out.writeInt( shipWorld.hangingEntities().size() );
			for( EntityHanging hangingEntity : shipWorld.hangingEntities().values() )
			{
				NBTTagCompound nbt = new NBTTagCompound();
				hangingEntity.writeToNBTOptional( nbt );
				NBTBase.writeNamedTag( nbt, out );
			}
		}
	};
	
	private static TreeMap<Integer,ShipWorldPersistence> m_versions;
	
	static
	{
		m_versions = new TreeMap<Integer,ShipWorldPersistence>();
		for( ShipWorldPersistence persistence : values() )
		{
			m_versions.put( persistence.m_version, persistence );
		}
	}
	
	private int m_version;
	
	private ShipWorldPersistence( int version )
	{
		m_version = version;
	}
	
	protected abstract ShipWorld onRead( World world, DataInputStream in ) throws IOException;
	protected abstract void onWrite( ShipWorld shipWorld, DataOutputStream out ) throws IOException;
	
	private static ShipWorldPersistence get( int version )
	{
		return m_versions.get( version );
	}
	
	private static ShipWorldPersistence getNewestVersion( )
	{
		return m_versions.lastEntry().getValue();
	}
	
	public static ShipWorld readAnyVersion( World world, byte[] data )
	throws UnrecognizedPersistenceVersion
	{
		try
		{
			return readAnyVersion( world, new ByteArrayInputStream( data ) );
		}
		catch( IOException ex )
		{
			// byte buffers should never throw an IOException, so writing a crap-ton of boilerplate code to handle
			// those exception is pretty ridiculous. Just rethrow as an error
			throw new Error( ex );
		}
	}
	
	public static ShipWorld readAnyVersion( World world, InputStream in )
	throws IOException, UnrecognizedPersistenceVersion
	{
		DataInputStream din = new DataInputStream( in );
		int version = din.readInt();
		ShipWorldPersistence persistence = get( version );
		if( persistence == null )
		{
			throw new UnrecognizedPersistenceVersion( version );
		}
		return persistence.onRead( world, din );
	}
	
	public static byte[] writeNewestVersion( ShipWorld shipWorld )
	{
		try
		{
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			writeNewestVersion( shipWorld, buf );
			return buf.toByteArray();
		}
		catch( IOException ex )
		{
			// byte buffers should never throw an IOException, so writing a crap-ton of boilerplate code to handle
			// those exception is pretty ridiculous. Just rethrow as an error
			throw new Error( ex );
		}
	}
	
	public static void writeNewestVersion( ShipWorld shipWorld, OutputStream out )
	throws IOException
	{
		DataOutputStream dout = new DataOutputStream( out );
		ShipWorldPersistence persistence = getNewestVersion();
		dout.writeInt( persistence.m_version );
		persistence.onWrite( shipWorld, dout );
	}
}
