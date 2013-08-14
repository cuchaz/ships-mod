package cuchaz.ships;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BoxCorner;
import cuchaz.modsShared.RotatedBB;
import cuchaz.modsShared.BlockUtils.BlockConditionValidator;
import cuchaz.modsShared.BoundingBoxInt;
import cuchaz.modsShared.Envelopes;

public class ShipGeometry
{
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
				if( !m_blocks.contains( coords ) )
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
		// first, get all boundary blocks
		final TreeSet<ChunkCoordinates> boundaryBlocks = new TreeSet<ChunkCoordinates>();
		ChunkCoordinates neighborCoords = new ChunkCoordinates( 0, 0, 0 );
		for( ChunkCoordinates coords : m_blocks )
		{
			// for each neighbor
			for( BlockSide side : BlockSide.values() )
			{
				neighborCoords.posX = coords.posX + side.getDx();
				neighborCoords.posY = coords.posY + side.getDy();
				neighborCoords.posZ = coords.posZ + side.getDz();
				
				// if it's not a ship block, it's a boundary block
				if( !m_blocks.contains( neighborCoords ) )
				{
					boundaryBlocks.add( new ChunkCoordinates( neighborCoords ) );
				}
			}
		}
		
		// boundaryBlocks will have some number of connected components. Find them all and classify each as inner/outer
		m_outerBoundaries = new ArrayList<TreeSet<ChunkCoordinates>>();
		m_holes = new ArrayList<TreeSet<ChunkCoordinates>>();
		for( TreeSet<ChunkCoordinates> component : BlockUtils.getConnectedComponents( boundaryBlocks ) )
		{
			// is this component the outer boundary?
			if( isConnectedToShell( component.first() ) )
			{
				m_outerBoundaries.add( component );
			}
			else
			{
				// compute the hole from the boundary
				m_holes.add( BlockUtils.getHoleFromInnerBoundary( component, m_blocks ) );
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
		int volume = ( box.getDx() + 3 )*( box.getDy() + 3 )*( box.getDz() + 3 );
		
		Boolean result = BlockUtils.searchForCondition(
			coords,
			volume,
			new BlockConditionValidator( )
			{
				@Override
				public boolean isValid( ChunkCoordinates coords )
				{
					return !m_blocks.contains( coords ) && ( maxY == null || coords.posY <= maxY );
				}

				@Override
				public boolean isConditionMet( ChunkCoordinates coords )
				{
					// is this a shell block?
					return !box.containsPoint( coords );
				}
			}
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
			
			// for each outer boundary component...
			List<TreeSet<ChunkCoordinates>> outerBoundariesUnderwater = new ArrayList<TreeSet<ChunkCoordinates>>();
			for( TreeSet<ChunkCoordinates> outerBoundary : m_outerBoundaries )
			{
				TreeSet<ChunkCoordinates> clippedOuterBoundary = BlockUtils.getBlocksAtYAndBelow( outerBoundary, waterLevel );
				if( !clippedOuterBoundary.isEmpty() )
				{
					outerBoundariesUnderwater.add( clippedOuterBoundary );
				}
			}
			
			for( TreeSet<ChunkCoordinates> component : outerBoundariesUnderwater )
			{
				// if the component is isolated from the shell, it's trapped air
				if( !isConnectedToShell( component.first(), waterLevel ) )
				{
					trappedAirAtThisWaterLevel.addAll( BlockUtils.getHoleFromInnerBoundary( component, m_blocks, waterLevel ) );
				}
			}
			
			m_trappedAir.put( waterLevel, trappedAirAtThisWaterLevel );
		}
	}
}
