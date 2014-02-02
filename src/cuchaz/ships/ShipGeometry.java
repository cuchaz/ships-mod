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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BlockUtils.BlockConditionChecker;
import cuchaz.modsShared.BlockUtils.BlockExplorer;
import cuchaz.modsShared.BlockUtils.Neighbors;
import cuchaz.modsShared.BoundingBoxInt;
import cuchaz.modsShared.BoxCorner;
import cuchaz.modsShared.Envelopes;
import cuchaz.modsShared.RotatedBB;

public class ShipGeometry
{
	public static final Neighbors ShipBlockNeighbors = Neighbors.Edges;
	public static final Neighbors VoidBlockNeighbors = Neighbors.Faces;
	
	private TreeSet<ChunkCoordinates> m_blocks;
	private Envelopes m_envelopes;
	private List<TreeSet<ChunkCoordinates>> m_outerBoundaries;
	private List<TreeSet<ChunkCoordinates>> m_holes;
	private TreeMap<Integer,TreeSet<ChunkCoordinates>> m_trappedAir;
	
	public ShipGeometry( Set<ChunkCoordinates> blocks )
	{
		m_blocks = new TreeSet<ChunkCoordinates>( blocks );
		
		m_envelopes = new Envelopes( m_blocks );
		m_outerBoundaries = null;
		m_holes = null;
		m_trappedAir = null;
		
		computeBoundaryAndHoles();
		computeTrappedAir();
	}
	
	public Envelopes getEnvelopes( )
	{
		return m_envelopes;
	}
	
	public List<TreeSet<ChunkCoordinates>> getOuterBoundaries( )
	{
		return m_outerBoundaries;
	}
	
	public List<TreeSet<ChunkCoordinates>> getHoles( )
	{
		return m_holes;
	}
	
	public TreeSet<ChunkCoordinates> getTrappedAir( int y )
	{
		// if y is too big, clamp it. ie when the ship is underwater, we get the max trapped air
		y = Math.min( y, m_trappedAir.lastKey() );
		TreeSet<ChunkCoordinates> coords = m_trappedAir.get( y );
		if( coords == null )
		{
			coords = new TreeSet<ChunkCoordinates>();
		}
		return coords;
	}
	
	public TreeSet<ChunkCoordinates> getTrappedAirFromWaterHeight( int waterHeightInBlockSpace )
	{
		// remember, the water height is the y-value of the surface of the water
		// it's always at the top of the water block
		// therefore, if we want the y coord of the top water block, we need to subtract 1
		return getTrappedAir( waterHeightInBlockSpace - 1 );
	}

	public TreeSet<ChunkCoordinates> getTrappedAirFromWaterHeight( double waterHeightInBlockSpace )
	{
		// for double water height values, round up to the top of the block, then subtract 1
		// or, just round down
		return getTrappedAir( MathHelper.floor_double( waterHeightInBlockSpace ) );
	}
	
	public List<ChunkCoordinates> rangeQuery( RotatedBB box )
	{
		// get the bounds in y
		int minY = MathHelper.floor_double( box.getMinY() );
		int maxY = MathHelper.floor_double( box.getMaxY() );
		
		List<ChunkCoordinates> blocks = new ArrayList<ChunkCoordinates>();
		for( int y=minY; y<=maxY; y++ )
		{
			// add up the blocks from the xz range query
			blocks.addAll( xzRangeQuery( y, box ) );
		}
		return blocks;
	}
	
	public List<ChunkCoordinates> xzRangeQuery( int y, RotatedBB box )
	{
		// UNDONE: we can probably optimize this using a better algorithm
		
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		
		// get the bounds in x and z
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for( BoxCorner corner : BlockSide.Top.getCorners() )
		{
			box.getCorner( p, corner );
			int x = MathHelper.floor_double( p.xCoord );
			int z = MathHelper.floor_double( p.zCoord );
			
			minX = Math.min( minX, x );
			maxX = Math.max( maxX, x );
			minZ = Math.min( minZ, z );
			maxZ = Math.max( maxZ, z );
		}
		
		// search over the blocks in the range
		ChunkCoordinates coords = new ChunkCoordinates();
		List<ChunkCoordinates> blocks = new ArrayList<ChunkCoordinates>();
		for( int x=minX; x<=maxX; x++ )
		{
			for( int z=minZ; z<=maxZ; z++ )
			{
				coords.set( x, y, z );
				
				// is there even a block here?
				if( m_blocks.contains( coords ) )
				{
					continue;
				}
				
				if( blockIntersectsBoxXZ( x, z, box ) )
				{
					blocks.add( new ChunkCoordinates( coords ) );
				}
			}
		}
		return blocks;
	}
	
	public List<ChunkCoordinates> rangeQuery( AxisAlignedBB box )
	{
		// get the block coordinate bounds for y
		int minY = MathHelper.floor_double( box.minY );
		int maxY = MathHelper.floor_double( box.maxY );
		
		List<ChunkCoordinates> blocks = new ArrayList<ChunkCoordinates>();
		for( int y=minY; y<=maxY; y++ )
		{
			blocks.addAll( rangeQuery( box, y ) );
		}
		return blocks;
	}
	
	public List<ChunkCoordinates> rangeQuery( AxisAlignedBB box, int y )
	{
		// get the block coordinate bounds for x and z
		int minX = MathHelper.floor_double( box.minX );
		int minZ = MathHelper.floor_double( box.minZ );
		int maxX = MathHelper.floor_double( box.maxX );
		int maxZ = MathHelper.floor_double( box.maxZ );
		
		ChunkCoordinates coords = new ChunkCoordinates();
		List<ChunkCoordinates> blocks = new ArrayList<ChunkCoordinates>();
		for( int x=minX; x<=maxX; x++ )
		{
			for( int z=minZ; z<=maxZ; z++ )
			{
				coords.set( x, y, z );
				if( m_blocks.contains( coords ) )
				{
					blocks.add( new ChunkCoordinates( coords ) );
				}
			}
		}
		return blocks;
	}
	
	private boolean blockIntersectsBoxXZ( int x, int z, RotatedBB box )
	{
		// return true if any xz corner of the block is in the rotated box
		double y = ( box.getMinY() + box.getMaxY() )/2;
		return box.containsPoint( x + 0, y, z + 0 )
			|| box.containsPoint( x + 0, y, z + 1 )
			|| box.containsPoint( x + 1, y, z + 0 )
			|| box.containsPoint( x + 1, y, z + 1 )
			|| anyCornerIsInBlockXZ( box, x, z );
	}
	
	private boolean anyCornerIsInBlockXZ( RotatedBB box, int x, int z )
	{
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		for( BoxCorner corner : BlockSide.Top.getCorners() )
		{
			box.getCorner( p, corner );
			if( isPointInBlockXZ( p.xCoord, p.zCoord, x, z ) )
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isPointInBlockXZ( double px, double pz, int blockX, int blockZ )
	{
		return px >= blockX && px <= blockX + 1
			&& pz >= blockZ && pz <= blockZ + 1;
	}
	
	private void computeBoundaryAndHoles( )
	{
		// first, get all blocks touching the ship on a face (aka the boundary)
		final TreeSet<ChunkCoordinates> boundaryBlocks = new TreeSet<ChunkCoordinates>();
		ChunkCoordinates neighborCoords = new ChunkCoordinates( 0, 0, 0 );
		for( ChunkCoordinates coords : m_blocks )
		{
			for( int i=0; i<VoidBlockNeighbors.getNumNeighbors(); i++ )
			{
				VoidBlockNeighbors.getNeighbor( neighborCoords, coords, i );
				if( !m_blocks.contains( neighborCoords ) )
				{
					boundaryBlocks.add( new ChunkCoordinates( neighborCoords ) );
				}
			}
		}
		
		// boundaryBlocks will have some number of connected components. Find them all and classify each as inner/outer
		m_outerBoundaries = new ArrayList<TreeSet<ChunkCoordinates>>();
		m_holes = new ArrayList<TreeSet<ChunkCoordinates>>();
		for( TreeSet<ChunkCoordinates> component : BlockUtils.getConnectedComponents( boundaryBlocks, VoidBlockNeighbors ) )
		{
			// is this component the outer boundary?
			if( isConnectedToShell( component.first() ) )
			{
				m_outerBoundaries.add( component );
			}
			else
			{
				// compute the hole from the boundary
				m_holes.add( BlockUtils.getHoleFromInnerBoundary( component, m_blocks, VoidBlockNeighbors ) );
			}
		}
	}
	
	private boolean isConnectedToShell( ChunkCoordinates coords )
	{
		return isConnectedToShell( coords, null );
	}
	
	private boolean isConnectedToShell( ChunkCoordinates coords, final Integer maxY )
	{
		// don't check more blocks than can fit in the shell
		final BoundingBoxInt box = m_envelopes.getBoundingBox();
		int shellVolume = ( box.getDx() + 2 )*( box.getDy() + 2 )*( box.getDz() + 2 );
		
		Boolean result = BlockUtils.searchForCondition(
			coords,
			shellVolume,
			new BlockConditionChecker( )
			{
				@Override
				public boolean isConditionMet( ChunkCoordinates coords )
				{
					// is this a shell block?
					return !box.containsPoint( coords );
				}
			},
			new BlockExplorer( )
			{
				@Override
				public boolean shouldExploreBlock( ChunkCoordinates coords )
				{
					return !m_blocks.contains( coords ) && ( maxY == null || coords.posY <= maxY );
				}
			},
			VoidBlockNeighbors
		);
		
		// just in case...
		if( result == null )
		{
			throw new Error( "We evaluated too many blocks checking for the shell. This shouldn't have happened." );
		}
		
		return result;
	}
	
	private void computeTrappedAir( )
	{
		// needs blocks and boundaries
		if( m_blocks == null )
		{
			throw new Error( "Need blocks!" );
		}
		if( m_outerBoundaries == null || m_holes == null )
		{
			throw new Error( "Need boundaries!" );
		}
		
		// get the y-range
		int minY = m_envelopes.getBoundingBox().minY;
		int maxY = m_envelopes.getBoundingBox().maxY;
		
		// check the ship layer-by layer starting from the bottom
		m_trappedAir = new TreeMap<Integer,TreeSet<ChunkCoordinates>>();
		for( int waterLevel=minY; waterLevel<=maxY+1; waterLevel++ )
		{
			TreeSet<ChunkCoordinates> trappedAirAtThisWaterLevel = new TreeSet<ChunkCoordinates>();
			
			// hole blocks are always trapped air
			for( Set<ChunkCoordinates> hole : m_holes )
			{
				trappedAirAtThisWaterLevel.addAll( BlockUtils.getBlocksAtYAndBelow( hole, waterLevel ) );
			}
			
			// get the outer boundary components under water (clip them when needed)
			List<TreeSet<ChunkCoordinates>> outerBoundariesUnderwater = new ArrayList<TreeSet<ChunkCoordinates>>();
			for( TreeSet<ChunkCoordinates> outerBoundary : m_outerBoundaries )
			{
				TreeSet<ChunkCoordinates> clippedOuterBoundary = BlockUtils.getBlocksAtYAndBelow( outerBoundary, waterLevel );
				for( TreeSet<ChunkCoordinates> clippedOuterBoundaryComponent : BlockUtils.getConnectedComponents( clippedOuterBoundary, VoidBlockNeighbors ) )
				{
					if( !clippedOuterBoundaryComponent.isEmpty() )
					{
						outerBoundariesUnderwater.add( clippedOuterBoundaryComponent );
					}
				}
			}
			
			for( TreeSet<ChunkCoordinates> component : outerBoundariesUnderwater )
			{
				// if the component is isolated from the shell, it's trapped air
				if( !isConnectedToShell( component.first(), waterLevel ) )
				{
					trappedAirAtThisWaterLevel.addAll( BlockUtils.getHoleFromInnerBoundary( component, m_blocks, VoidBlockNeighbors, waterLevel ) );
				}
			}
			
			m_trappedAir.put( waterLevel, trappedAirAtThisWaterLevel );
		}
	}
}
