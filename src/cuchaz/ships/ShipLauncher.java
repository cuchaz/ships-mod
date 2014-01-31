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

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BlockUtils.UpdateRules;
import cuchaz.modsShared.BoundingBoxInt;
import cuchaz.modsShared.Envelopes;

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
	private int m_x;
	private int m_y;
	private int m_z;
	private ShipType m_shipType;
	private List<ChunkCoordinates> m_blocks; // NOTE: blocks are in world coordinates
	private List<Boolean> m_launchFlags;
	private ShipWorld m_shipWorld;
	private ShipPhysics m_shipPhysics;
	private Double m_equilibriumWaterHeight;
	private int m_numBlocksChecked;
	
	public ShipLauncher( final World world, int x, int y, int z )
	{
		m_world = world;
		m_x = x;
		m_y = y;
		m_z = z;
		
		// get the ship type from the block
		m_shipType = Ships.m_blockShip.getShipType( world, x, y, z );
		
		// determine how many blocks to check
		int numBlocksToCheck = getNumBlocksToCheck();
		
		// find all the blocks connected to the ship block
		m_blocks = BlockUtils.searchForBlocks(
			x, y, z,
			numBlocksToCheck,
			new BlockUtils.BlockValidator( )
			{
				@Override
				public boolean isValid( ChunkCoordinates coords )
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
				m_blocks.add( new ChunkCoordinates( m_x, m_y, m_z ) );
				
				m_shipWorld = new ShipWorld( m_world, new ChunkCoordinates( m_x, m_y, m_z ), m_blocks );
				m_shipPhysics = new ShipPhysics( m_shipWorld.getBlocksStorage() );
				m_equilibriumWaterHeight = m_shipPhysics.getEquilibriumWaterHeight();
			}
		}
		else
		{
			// we found WAY too many blocks
			m_shipWorld = null;
			m_shipPhysics = null;
			m_equilibriumWaterHeight = null;
			m_numBlocksChecked = numBlocksToCheck;
		}
		
		// compute the launch flags
		m_launchFlags = new ArrayList<Boolean>();
		for( LaunchFlag flag : LaunchFlag.values() )
		{
			m_launchFlags.add( flag.computeValue( this ) );
		}
	}
	
	public int getX( )
	{
		return m_x;
	}
	
	public int getY( )
	{
		return m_y;
	}
	
	public int getZ( )
	{
		return m_z;
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
	
	public EntityShip launch( )
	{
		// currently, this is only called on the server
		assert( !m_world.isRemote );
		
		Vec3 centerOfMass = new ShipPhysics( m_shipWorld.getBlocksStorage() ).getCenterOfMass();
		
		// spawn a ship entity
		EntityShip ship = new EntityShip( m_world );
		ship.setPositionAndRotation(
			m_x + centerOfMass.xCoord,
			m_y + centerOfMass.yCoord,
			m_z + centerOfMass.zCoord,
			0, 0
		);
		ship.setShipWorld( m_shipWorld );
		
		if( !m_world.spawnEntityInWorld( ship ) )
		{
			Ships.logger.warning( String.format( "Could not spawn ship in world at (%d,%d,%d)", ship.posX, ship.posY, ship.posZ ) );
			return null;
		}
		
		// remove the world blocks, but don't tell the clients. They'll do it later
		int waterHeight = computeWaterHeight();
		for( ChunkCoordinates coords : m_blocks )
		{
			if( coords.posY < waterHeight )
			{
				BlockUtils.changeBlockWithoutNotifyingIt( m_world, coords.posX, coords.posY, coords.posZ, Block.waterStill.blockID, 0, UpdateRules.UpdateNoOne );
			}
			else
			{
				BlockUtils.removeBlockWithoutNotifyingIt( m_world, coords.posX, coords.posY, coords.posZ, UpdateRules.UpdateNoOne );
			}
		}
		
		return ship;
	}
	
	private int computeWaterHeight( )
	{
		int maxWaterHeight = 0;
		
		// for each column in the ship or outside it
		Envelopes envelopes = m_shipWorld.getGeometry().getEnvelopes();
		for( int x=envelopes.getBoundingBox().minX-1; x<=envelopes.getBoundingBox().maxX+1; x++ )
		{
			for( int z=envelopes.getBoundingBox().minZ-1; z<=envelopes.getBoundingBox().maxZ+1; z++ )
			{
				int waterHeight = computeWaterHeight( x, z );
				if( waterHeight > maxWaterHeight )
				{
					maxWaterHeight = waterHeight;
				}
				
			}
		}
		return maxWaterHeight;
	}
	
	private int computeWaterHeight( int blockX, int blockZ )
	{
		// start at the top of the box
		Envelopes envelopes = m_shipWorld.getGeometry().getEnvelopes();
		int x = blockX + m_x;
		int y = envelopes.getBoundingBox().maxY+1 + m_y;
		int z = blockZ + m_z;
		
		// drop until we hit air
		boolean foundAir = false;
		for( ; y>=0; y-- )
		{
			if( m_world.getBlockMaterial( x, y, z ) == Material.air )
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
			if( m_world.getBlockMaterial( x, y, z ).isLiquid() )
			{
				// add 1 to return the entityY sense instead of the blockY sense
				return y + 1;
			}
		}
		
		return -1;
	}
}
