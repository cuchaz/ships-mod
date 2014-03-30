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
import java.util.TreeMap;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSetHeightIndex;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.BlockConditionChecker;
import cuchaz.modsShared.blocks.BlockUtils.BlockExplorer;
import cuchaz.modsShared.blocks.BlockUtils.Neighbors;
import cuchaz.modsShared.blocks.BoundingBoxInt;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.modsShared.blocks.Envelopes;
import cuchaz.modsShared.math.BoxCorner;
import cuchaz.modsShared.math.RotatedBB;

public class ShipGeometry
{
	public static final Neighbors ShipBlockNeighbors = Neighbors.Edges;
	public static final Neighbors VoidBlockNeighbors = Neighbors.Edges;
	
	private static class ClassifiedSegment
	{
		BlockSet segment;
		boolean isTrapped;
	}
	
	private BlockSet m_blocks;
	private Envelopes m_envelopes;
	private BlockSet m_outerBoundary;
	private List<BlockSet> m_holes;
	private TreeMap<Integer,BlockSet> m_trappedAir;
	
	public ShipGeometry( BlockSet blocks )
	{
		m_blocks = new BlockSet( blocks );
		
		m_envelopes = new Envelopes( m_blocks );
		m_outerBoundary = null;
		m_holes = null;
		m_trappedAir = null;
		
		computeBoundaryAndHoles();
		computeTrappedAir();
	}
	
	public Envelopes getEnvelopes( )
	{
		return m_envelopes;
	}
	
	public BlockSet getOuterBoundary( )
	{
		return m_outerBoundary;
	}
	
	public List<BlockSet> getHoles( )
	{
		return m_holes;
	}
	
	public BlockSet getTrappedAir( int y )
	{
		// if y is too big, clamp it. ie when the ship is underwater, we get the max trapped air
		y = Math.min( y, m_trappedAir.lastKey() );
		BlockSet coords = m_trappedAir.get( y );
		if( coords == null )
		{
			coords = new BlockSet();
		}
		return coords;
	}
	
	public BlockSet getTrappedAirFromWaterHeight( int waterHeightInBlockSpace )
	{
		// remember, the water height is the y-value of the surface of the water
		// it's always at the top of the water block
		// therefore, if we want the y coord of the top water block, we need to subtract 1
		return getTrappedAir( waterHeightInBlockSpace - 1 );
	}

	public BlockSet getTrappedAirFromWaterHeight( double waterHeightInBlockSpace )
	{
		// for double water height values, round up to the top of the block, then subtract 1
		// or, just round down
		return getTrappedAir( MathHelper.floor_double( waterHeightInBlockSpace ) );
	}
	
	public BlockSet rangeQuery( RotatedBB box )
	{
		// get the bounds in y
		int minY = MathHelper.floor_double( box.getMinY() );
		int maxY = MathHelper.floor_double( box.getMaxY() );
		
		BlockSet blocks = new BlockSet();
		for( int y=minY; y<=maxY; y++ )
		{
			// add up the blocks from the xz range query
			blocks.addAll( xzRangeQuery( y, box ) );
		}
		return blocks;
	}
	
	public BlockSet xzRangeQuery( int y, RotatedBB box )
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
		Coords coords = new Coords();
		BlockSet blocks = new BlockSet();
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
					blocks.add( new Coords( coords ) );
				}
			}
		}
		return blocks;
	}
	
	public BlockSet rangeQuery( AxisAlignedBB box )
	{
		// get the block coordinate bounds for y
		int minY = MathHelper.floor_double( box.minY );
		int maxY = MathHelper.floor_double( box.maxY );
		
		BlockSet blocks = new BlockSet();
		for( int y=minY; y<=maxY; y++ )
		{
			blocks.addAll( rangeQuery( box, y ) );
		}
		return blocks;
	}
	
	public BlockSet rangeQuery( AxisAlignedBB box, int y )
	{
		// get the block coordinate bounds for x and z
		int minX = MathHelper.floor_double( box.minX );
		int minZ = MathHelper.floor_double( box.minZ );
		int maxX = MathHelper.floor_double( box.maxX );
		int maxZ = MathHelper.floor_double( box.maxZ );
		
		Coords coords = new Coords();
		BlockSet blocks = new BlockSet();
		for( int x=minX; x<=maxX; x++ )
		{
			for( int z=minZ; z<=maxZ; z++ )
			{
				coords.set( x, y, z );
				if( m_blocks.contains( coords ) )
				{
					blocks.add( new Coords( coords ) );
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
		final BlockSet boundaryBlocks = new BlockSet();
		Coords neighborCoords = new Coords( 0, 0, 0 );
		for( Coords coords : m_blocks )
		{
			for( int i=0; i<VoidBlockNeighbors.getNumNeighbors(); i++ )
			{
				VoidBlockNeighbors.getNeighbor( neighborCoords, coords, i );
				if( !m_blocks.contains( neighborCoords ) )
				{
					boundaryBlocks.add( new Coords( neighborCoords ) );
				}
			}
		}
		
		// boundaryBlocks will have some number of connected components. Find them all and classify each as inner/outer
		m_holes = new ArrayList<BlockSet>();
		for( BlockSet component : BlockUtils.getConnectedComponents( boundaryBlocks, VoidBlockNeighbors ) )
		{
			// is this component the outer boundary?
			if( isConnectedToShell( component.first() ) )
			{
				assert( m_outerBoundary == null );
				m_outerBoundary = component;
			}
			else
			{
				// compute the hole from the boundary
				m_holes.add( BlockUtils.getHoleFromInnerBoundary( component, m_blocks, VoidBlockNeighbors ) );
			}
		}
	}
	
	private boolean isConnectedToShell( Coords coords )
	{
		return isConnectedToShell( coords, new BlockSet() );
	}
	
	private boolean isConnectedToShell( Coords coords, BlockSet shellExtra )
	{
		return isConnectedToShell( coords, shellExtra, null );
	}
	
	private boolean isConnectedToShell( Coords coords, Integer maxY )
	{
		return isConnectedToShell( coords, new BlockSet(), maxY );
	}
	
	private boolean isConnectedToShell( Coords coords, final BlockSet shellExtra, final Integer maxY )
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
				public boolean isConditionMet( Coords coords )
				{
					// is this a shell block?
					return !box.containsPoint( coords ) || shellExtra.contains( coords );
				}
			},
			new BlockExplorer( )
			{
				@Override
				public boolean shouldExploreBlock( Coords coords )
				{
					return ( maxY == null || coords.y <= maxY ) && !m_blocks.contains( coords );
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
		// needs blocks and boundary
		if( m_blocks == null )
		{
			throw new Error( "Need blocks!" );
		}
		if( m_outerBoundary == null || m_holes == null )
		{
			throw new Error( "Need boundaries!" );
		}
		
		// get the y-range
		int minY = m_envelopes.getBoundingBox().minY;
		int maxY = m_envelopes.getBoundingBox().maxY;
		
		m_trappedAir = new TreeMap<Integer,BlockSet>();
		
		// analyze the outer boundary
		BlockSetHeightIndex boundaryIndex = new BlockSetHeightIndex( m_outerBoundary );
		List<ClassifiedSegment> boundarySegments = new ArrayList<ClassifiedSegment>();
		for( int y=minY; y<=maxY+1; y++ )
		{
			// get all the segments at y
			List<BlockSet> ySegments = BlockUtils.getConnectedComponents( boundaryIndex.get( y ), VoidBlockNeighbors );
			
			// merge them with existing segments
			for( BlockSet ySegment : ySegments )
			{
				List<ClassifiedSegment> connectedSegments = getYConnectedSegments( ySegment, boundarySegments, VoidBlockNeighbors );
				if( connectedSegments.isEmpty() )
				{
					// add the new segment
					ClassifiedSegment classifiedSegment = new ClassifiedSegment();
					classifiedSegment.segment = ySegment;
					classifiedSegment.isTrapped = !isConnectedToShell( ySegment.first(), y );
					boundarySegments.add( classifiedSegment );
				}
				else
				{
					// merge all the existing segments
					ClassifiedSegment baseSegment = connectedSegments.get( 0 );
					for( int i=1; i<connectedSegments.size(); i++ )
					{
						ClassifiedSegment nextSegment = connectedSegments.get( i );
						baseSegment.isTrapped = baseSegment.isTrapped && nextSegment.isTrapped;
						baseSegment.segment.addAll( nextSegment.segment );
						boundarySegments.remove( nextSegment );
					}
					baseSegment.segment.addAll( ySegment );
				}
			}
			
			BlockSet trappedAirUpToThisY = new BlockSet();
			m_trappedAir.put( y, trappedAirUpToThisY );
			
			// compute the trapped air so far
			for( ClassifiedSegment segment : boundarySegments )
			{
				if( segment.isTrapped )
				{
					trappedAirUpToThisY.addAll( segment.segment );
				}
			}
		}
		
		// analyze the holes
		BlockSetHeightIndex holeIndex = new BlockSetHeightIndex();
		for( BlockSet hole : m_holes )
		{
			holeIndex.add( hole );
		}
		BlockSet holeBlocks = new BlockSet();
		for( int y=minY; y<=maxY+1; y++ )
		{
			// add the blocks for this y
			BlockSet layer = holeIndex.get( y );
			if( layer != null )
			{
				holeBlocks.addAll( layer );
			}
			
			m_trappedAir.get( y ).addAll( holeBlocks );
		}
	}
	
	private List<ClassifiedSegment> getYConnectedSegments( BlockSet ySegment, Iterable<ClassifiedSegment> segments, Neighbors neighbors )
	{
		List<ClassifiedSegment> connectedSegments = new ArrayList<ClassifiedSegment>();
		for( ClassifiedSegment segment : segments )
		{
			if( isYConnected( segment.segment, ySegment, neighbors ) )
			{
				connectedSegments.add( segment );
			}
		}
		return connectedSegments;
	}
	
	private boolean isYConnected( BlockSet below, BlockSet at, Neighbors neighbors )
	{
		// NOTE: below is strictly below y, at is strictly at y
		Coords neighborCoords = new Coords( 0, 0, 0 );
		for( Coords coords : at )
		{
			for( int i=0; i<neighbors.getNumNeighbors(); i++ )
			{
				neighbors.getNeighbor( neighborCoords, coords, i );
				if( neighborCoords.y == coords.y - 1 && below.contains( neighborCoords ) )
				{
					return true;
				}
			}
		}
		return false;
	}
}
