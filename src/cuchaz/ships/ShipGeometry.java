package cuchaz.ships;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BlockUtils.BlockConditionValidator;

import net.minecraft.util.ChunkCoordinates;

public class ShipGeometry
{
	private TreeSet<ChunkCoordinates> m_blocks;
	private List<TreeSet<ChunkCoordinates>> m_outerBoundaries;
	private List<TreeSet<ChunkCoordinates>> m_holes;
	private TreeMap<Integer,TreeSet<ChunkCoordinates>> m_trappedAir;
	
	public ShipGeometry( Set<ChunkCoordinates> blocks )
	{
		m_blocks = new TreeSet<ChunkCoordinates>( blocks );
		
		m_outerBoundaries = null;
		m_holes = null;
		m_trappedAir = null;
		
		computeBoundaryAndHoles();
		computeTrappedAir();
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
		return m_trappedAir.get( y );
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
		final ChunkCoordinates min = m_blocks.first();
		final ChunkCoordinates max = m_blocks.last();
		int volume = ( max.posX - min.posX + 3 ) * ( max.posY - min.posY + 3 ) * ( max.posZ - min.posZ + 3 );
		
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
					return coords.posX < min.posX || coords.posX > max.posX
						|| coords.posY < min.posY || coords.posY > max.posY
						|| coords.posZ < min.posZ || coords.posZ > max.posZ;
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
		int minY = m_blocks.first().posY;
		int maxY = m_blocks.last().posY;
		
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
