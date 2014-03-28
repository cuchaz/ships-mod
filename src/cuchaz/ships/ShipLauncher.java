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
package cuchaz.ships;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityHanging;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.PacketDispatcher;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockArray;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.BlockExplorer;
import cuchaz.modsShared.blocks.BlockUtils.UpdateRules;
import cuchaz.modsShared.blocks.BoundingBoxInt;
import cuchaz.modsShared.blocks.Envelopes;
import cuchaz.ships.packets.PacketShipLaunched;

public class ShipLauncher
{
	public static enum LaunchFlag
	{
		RightNumberOfBlocks
		{
			@Override
			public boolean computeValue( ShipLauncher launcher )
			{
				return launcher.m_blocks != null
					&& !launcher.m_blocks.isEmpty()
					&& launcher.m_blocks.size() <= launcher.m_shipType.getMaxNumBlocks() + 1; // +1 since the ship block is free
			}
		},
		WillItFloat
		{
			@Override
			public boolean computeValue( ShipLauncher launcher )
			{
				if( launcher.m_blocks == null || launcher.m_equilibriumWaterHeight == null )
				{
					return false;
				}
				
				return launcher.m_equilibriumWaterHeight < launcher.getShipBoundingBox().maxY + 1;
			}
		};
		
		public abstract boolean computeValue( ShipLauncher launcher );
	}
	
	private World m_world;
	private ChunkCoordinates m_shipBlock;
	private ShipType m_shipType;
	private BlockSet m_blocks; // NOTE: blocks are in world coordinates
	private List<Boolean> m_launchFlags;
	private ShipWorld m_shipWorld;
	private ShipPhysics m_shipPhysics;
	private Double m_equilibriumWaterHeight;
	private Double m_sinkWaterHeight;
	private int m_numBlocksChecked;
	
	public ShipLauncher( final World world, ChunkCoordinates shipBlock )
	{
		m_world = world;
		m_shipBlock = shipBlock;
		
		// get the ship type from the block
		m_shipType = Ships.m_blockShip.getShipType( world, m_shipBlock.posX, m_shipBlock.posY, m_shipBlock.posZ );
		
		// determine how many blocks to check
		int numBlocksToCheck = getNumBlocksToCheck();
		
		// find all the blocks connected to the ship block
		m_blocks = BlockUtils.searchForBlocks(
			m_shipBlock.posX, m_shipBlock.posY, m_shipBlock.posZ,
			numBlocksToCheck,
			new BlockExplorer( )
			{
				@Override
				public boolean shouldExploreBlock( ChunkCoordinates coords )
				{
					return !MaterialProperties.isSeparatorBlock( Block.blocksList[world.getBlockId( coords.posX, coords.posY, coords.posZ )] );
				}
			},
			ShipGeometry.ShipBlockNeighbors
		);
		
		if( m_blocks != null )
		{
			m_numBlocksChecked = m_blocks.size();
			
			// did we find too many blocks?
			if( m_blocks.size() > m_shipType.getMaxNumBlocks() )
			{
				m_blocks = null;
			}
			else
			{
				// also add the ship block
				m_blocks.add( m_shipBlock );
				
				m_shipWorld = new ShipWorld( m_world, m_shipBlock, m_blocks );
				m_shipPhysics = new ShipPhysics( m_shipWorld.getBlocksStorage() );
				m_equilibriumWaterHeight = m_shipPhysics.getEquilibriumWaterHeight();
				m_sinkWaterHeight = m_shipPhysics.getSinkWaterHeight();
			}
		}
		else
		{
			// we found WAY too many blocks
			m_shipWorld = null;
			m_shipPhysics = null;
			m_equilibriumWaterHeight = null;
			m_sinkWaterHeight = null;
			m_numBlocksChecked = numBlocksToCheck;
		}
		
		// compute the launch flags
		m_launchFlags = new ArrayList<Boolean>();
		for( LaunchFlag flag : LaunchFlag.values() )
		{
			m_launchFlags.add( flag.computeValue( this ) );
		}
	}
	
	public ChunkCoordinates getShipBlock( )
	{
		return m_shipBlock;
	}
	
	public ShipType getShipType( )
	{
		return m_shipType;
	}
	
	public ShipWorld getShipWorld( )
	{
		return m_shipWorld;
	}
	
	public ShipPhysics getShipPhysics( )
	{
		return m_shipPhysics;
	}
	
	public int getNumBlocks( )
	{
		// don't count the ship block towards the size quota
		return m_blocks.size() - 1;
	}
	
	public int getNumBlocksChecked( )
	{
		return m_numBlocksChecked;
	}
	
	public int getNumBlocksToCheck( )
	{
		return Math.max( 100, m_shipType.getMaxNumBlocks()*2 );
	}
	
	public boolean getLaunchFlag( LaunchFlag flag )
	{
		return m_launchFlags.get( flag.ordinal() );
	}
	
	public boolean isLaunchable( )
	{
		boolean isValid = true;
		for( LaunchFlag flag : LaunchFlag.values() )
		{
			isValid = isValid && getLaunchFlag( flag );
		}
		return isValid;
	}
	
	public BlockSide getShipSide( )
	{
		if( getShipBoundingBox() == null )
		{
			return null;
		}
		
		// return the widest side of north,west
		if( getShipBoundingBox().getDx() > getShipBoundingBox().getDz() )
		{
			return BlockSide.North;
		}
		return BlockSide.West;
	}
	
	public BlockSide getShipFront( )
	{
		if( getShipBoundingBox() == null )
		{
			return null;
		}
		
		// return the thinnest side of north,west
		if( getShipBoundingBox().getDx() > getShipBoundingBox().getDz() )
		{
			return BlockSide.North;
		}
		return BlockSide.West;
	}
	
	public BoundingBoxInt getShipBoundingBox( )
	{
		if( m_shipWorld == null )
		{
			return null;
		}
		
		return m_shipWorld.getGeometry().getEnvelopes().getBoundingBox();
	}
	
	public BlockArray getShipEnvelope( BlockSide side )
	{
		if( m_shipWorld == null )
		{
			return null;
		}
		
		return m_shipWorld.getGeometry().getEnvelopes().getEnvelope( side );
	}
	
	public Double getEquilibriumWaterHeight( )
	{
		return m_equilibriumWaterHeight;
	}
	
	public Double getSinkWaterHeight( )
	{
		return m_sinkWaterHeight;
	}
	
	public EntityShip launch( )
	{
		// currently, this is only called on the server
		assert( Environment.isServer() );
		
		// spawn the ship
		EntityShip ship = new EntityShip( m_world );
		initShip( ship, m_shipWorld, m_shipBlock );
		
		if( !m_world.spawnEntityInWorld( ship ) )
		{
			Ships.logger.warning( "Could not spawn ship in world at (%.2f,%.2f,%.2f)", ship.posX, ship.posY, ship.posZ );
			return null;
		}
		
		// tell clients the ship launched
		PacketDispatcher.sendPacketToAllPlayers( new PacketShipLaunched( ship, m_shipBlock ).getCustomPacket() );
		
		return ship;
	}
	
	public static void initShip( EntityShip ship, ShipWorld shipWorld, ChunkCoordinates shipBlock )
	{
		Vec3 centerOfMass = new ShipPhysics( shipWorld.getBlocksStorage() ).getCenterOfMass();
		
		// set ship properties
		ship.setPositionAndRotation(
			shipBlock.posX + centerOfMass.xCoord,
			shipBlock.posY + centerOfMass.yCoord,
			shipBlock.posZ + centerOfMass.zCoord,
			0, 0
		);
		ship.setShipWorld( shipWorld );
		
		removeShipFromWorld( ship.worldObj, shipWorld, shipBlock, UpdateRules.UpdateNoOne );
	}
	
	public static void removeShipFromWorld( World world, ShipWorld shipWorld, ChunkCoordinates shipBlock, UpdateRules updateRules )
	{
		// translate the ship blocks into world space
		BlockSet worldBlocks = new BlockSet();
		for( ChunkCoordinates blockCoords : shipWorld.coords() )
		{
			worldBlocks.add( new ChunkCoordinates(
				blockCoords.posX + shipBlock.posX,
				blockCoords.posY + shipBlock.posY,
				blockCoords.posZ + shipBlock.posZ
			) );
		}
		
		// compute the water height
		int waterHeight = computeWaterHeight( world, shipWorld, shipBlock );
		
		// remove the world blocks, but don't tell the clients. They'll do it later when the ship blocks are sent over
		for( ChunkCoordinates cords : worldBlocks )
		{
			if( cords.posY < waterHeight )
			{
				BlockUtils.changeBlockWithoutNotifyingIt( world, cords.posX, cords.posY, cords.posZ, Block.waterStill.blockID, 0, updateRules );
			}
			else
			{
				BlockUtils.removeBlockWithoutNotifyingIt( world, cords.posX, cords.posY, cords.posZ, updateRules );
			}
		}
		
		// restore the trapped air to water
		ChunkCoordinates worldCoords = new ChunkCoordinates( 0, 0, 0 );
		for( ChunkCoordinates blockCoords : shipWorld.getGeometry().getTrappedAirFromWaterHeight( waterHeight - shipBlock.posY ) )
		{
			worldCoords.posX = blockCoords.posX + shipBlock.posX;
			worldCoords.posY = blockCoords.posY + shipBlock.posY;
			worldCoords.posZ = blockCoords.posZ + shipBlock.posZ;
			BlockUtils.changeBlockWithoutNotifyingIt( world, worldCoords.posX, worldCoords.posY, worldCoords.posZ, Block.waterStill.blockID, 0, UpdateRules.UpdateNoOne );
		}
		
		// remove any hanging entities
		for( Map.Entry<ChunkCoordinates,EntityHanging> entry : shipWorld.getNearbyHangingEntities( world, worldBlocks ).entrySet() )
		{
			EntityHanging hangingEntity = entry.getValue();
			
			// remove the hanging entity from the world
			world.removeEntity( hangingEntity );
		}
	}
	
	private static int computeWaterHeight( World world, ShipWorld shipWorld, ChunkCoordinates shipBlock )
	{
		int maxWaterHeight = 0;
		
		// for each column in the ship or outside it
		Envelopes envelopes = shipWorld.getGeometry().getEnvelopes();
		for( int x=envelopes.getBoundingBox().minX-1; x<=envelopes.getBoundingBox().maxX+1; x++ )
		{
			for( int z=envelopes.getBoundingBox().minZ-1; z<=envelopes.getBoundingBox().maxZ+1; z++ )
			{
				int waterHeight = computeWaterHeight( world, shipWorld, shipBlock, x, z );
				if( waterHeight > maxWaterHeight )
				{
					maxWaterHeight = waterHeight;
				}
				
			}
		}
		return maxWaterHeight;
	}
	
	private static int computeWaterHeight( World world, ShipWorld shipWorld, ChunkCoordinates shipBlock, int blockX, int blockZ )
	{
		// start at the top of the box
		Envelopes envelopes = shipWorld.getGeometry().getEnvelopes();
		int x = blockX + shipBlock.posX;
		int y = envelopes.getBoundingBox().maxY+1 + shipBlock.posY;
		int z = blockZ + shipBlock.posZ;
		
		// drop until we hit air
		boolean foundAir = false;
		for( ; y>=0; y-- )
		{
			if( world.getBlockMaterial( x, y, z ) == Material.air )
			{
				foundAir = true;
				break;
			}
		}
		
		if( !foundAir )
		{
			return -1;
		}
		
		// keep dropping until we hit water
		for( ; y>=0; y-- )
		{
			if( world.getBlockMaterial( x, y, z ).isLiquid() )
			{
				// add 1 to return the entityY sense instead of the blockY sense
				return y + 1;
			}
		}
		
		return -1;
	}
}
