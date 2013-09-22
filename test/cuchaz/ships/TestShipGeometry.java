package cuchaz.ships;

import static org.junit.Assert.*;

import java.util.TreeSet;

import net.minecraft.util.ChunkCoordinates;

import org.junit.Test;

public class TestShipGeometry
{
	@Test
	public void singleBlockOuterBoudaries( )
	{
		ShipGeometry geometry = new ShipGeometry( getBlocks(
			0, 0, 0
		) );
		
		// check outer boundaries
		assertEquals( 6, geometry.getOuterBoundaries().size() );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( -1, 0, 0 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 1, 0, 0 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 0, -1, 0 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 0, 1, 0 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 0, 0, -1 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 0, 0, 1 ) ) );
		
		// check holes
		assertEquals( 0, geometry.getHoles().size() );
		
		// check trapped air
		assertEquals( 0, geometry.getTrappedAir( 0 ).size() );
		assertEquals( 0, geometry.getTrappedAir( 1 ).size() );
	}
	
	@Test
	public void singleBlockHole( )
	{
		ShipGeometry geometry = new ShipGeometry( getBlocks(
			0, 0, 0,   0, 0, 1,   0, 0, 2,
			1, 0, 0,   1, 0, 1,   1, 0, 2,
			2, 0, 0,   2, 0, 1,   2, 0, 2,
			
			0, 1, 0,   0, 1, 1,   0, 1, 2,
			1, 1, 0,              1, 1, 2,
			2, 1, 0,   2, 1, 1,   2, 1, 2,
			
			0, 2, 0,   0, 2, 1,   0, 2, 2,
			1, 2, 0,   1, 2, 1,   1, 2, 2,
			2, 2, 0,   2, 2, 1,   2, 2, 2
		) );
		
		// check outer boundaries
		assertEquals( 6, geometry.getOuterBoundaries().size() );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, -1, 0,   0, -1, 1,   0, -1, 2,
			1, -1, 0,   1, -1, 1,   1, -1, 2,
			2, -1, 0,   2, -1, 1,   2, -1, 2
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, 3, 0,   0, 3, 1,   0, 3, 2,
			1, 3, 0,   1, 3, 1,   1, 3, 2,
			2, 3, 0,   2, 3, 1,   2, 3, 2
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			3, 0, 0,   3, 0, 1,   3, 0, 2,
			3, 1, 0,   3, 1, 1,   3, 1, 2,
			3, 2, 0,   3, 2, 1,   3, 2, 2
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			-1, 0, 0,   -1, 0, 1,   -1, 0, 2,
			-1, 1, 0,   -1, 1, 1,   -1, 1, 2,
			-1, 2, 0,   -1, 2, 1,   -1, 2, 2
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, 0, 3,   1, 0, 3,   2, 0, 3,
			0, 1, 3,   1, 1, 3,   2, 1, 3,
			0, 2, 3,   1, 2, 3,   2, 2, 3
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, 0, -1,   1, 0, -1,   2, 0, -1,
			0, 1, -1,   1, 1, -1,   2, 1, -1,
			0, 2, -1,   1, 2, -1,   2, 2, -1
		) ) );
		
		// check holes
		assertEquals( 1, geometry.getHoles().size() );
		assertTrue( geometry.getHoles().contains( getBlocks( 1, 1, 1 ) ) );
		
		// check trapped air
		assertEquals( 0, geometry.getTrappedAir( 0 ).size() );
		assertEquals( 1, geometry.getTrappedAir( 1 ).size() );
		assertEquals( getBlocks( 1, 1, 1 ), geometry.getTrappedAir( 1 ) );
		assertEquals( 1, geometry.getTrappedAir( 2 ).size() );
		assertEquals( getBlocks( 1, 1, 1 ), geometry.getTrappedAir( 2 ) );
		assertEquals( 1, geometry.getTrappedAir( 3 ).size() );
		assertEquals( getBlocks( 1, 1, 1 ), geometry.getTrappedAir( 3 ) );
	}
	
	@Test
	public void threeByThreeByThreeHole( )
	{
		ShipGeometry geometry = new ShipGeometry( getBlocks(
			0, 0, 0,   0, 0, 1,   0, 0, 2,   0, 0, 3,   0, 0, 4,
			1, 0, 0,   1, 0, 1,   1, 0, 2,   1, 0, 3,   1, 0, 4,
			2, 0, 0,   2, 0, 1,   2, 0, 2,   2, 0, 3,   2, 0, 4,
			3, 0, 0,   3, 0, 1,   3, 0, 2,   3, 0, 3,   3, 0, 4,
			4, 0, 0,   4, 0, 1,   4, 0, 2,   4, 0, 3,   4, 0, 4,
			
			0, 1, 0,   0, 1, 1,   0, 1, 2,   0, 1, 3,   0, 1, 4,
			1, 1, 0,                                    1, 1, 4,
			2, 1, 0,                                    2, 1, 4,
			3, 1, 0,                                    3, 1, 4,
			4, 1, 0,   4, 1, 1,   4, 1, 2,   4, 1, 3,   4, 1, 4,
			
			0, 2, 0,   0, 2, 1,   0, 2, 2,   0, 2, 3,   0, 2, 4,
			1, 2, 0,                                    1, 2, 4,
			2, 2, 0,                                    2, 2, 4,
			3, 2, 0,                                    3, 2, 4,
			4, 2, 0,   4, 2, 1,   4, 2, 2,   4, 2, 3,   4, 2, 4,
			
			0, 3, 0,   0, 3, 1,   0, 3, 2,   0, 3, 3,   0, 3, 4,
			1, 3, 0,                                    1, 3, 4,
			2, 3, 0,                                    2, 3, 4,
			3, 3, 0,                                    3, 3, 4,
			4, 3, 0,   4, 3, 1,   4, 3, 2,   4, 3, 3,   4, 3, 4,
			
			0, 4, 0,   0, 4, 1,   0, 4, 2,   0, 4, 3,   0, 4, 4,
			1, 4, 0,   1, 4, 1,   1, 4, 2,   1, 4, 3,   1, 4, 4,
			2, 4, 0,   2, 4, 1,   2, 4, 2,   2, 4, 3,   2, 4, 4,
			3, 4, 0,   3, 4, 1,   3, 4, 2,   3, 4, 3,   3, 4, 4,
			4, 4, 0,   4, 4, 1,   4, 4, 2,   4, 4, 3,   4, 4, 4
		) );
		
		// don't bother with outer boundaries
		
		// check holes
		assertEquals( 1, geometry.getHoles().size() );
		assertTrue( geometry.getHoles().contains( getBlocks(
			1, 1, 1,   1, 1, 2,   1, 1, 3,
			2, 1, 1,   2, 1, 2,   2, 1, 3,
			3, 1, 1,   3, 1, 2,   3, 1, 3,
			
			1, 2, 1,   1, 2, 2,   1, 2, 3,
			2, 2, 1,   2, 2, 2,   2, 2, 3,
			3, 2, 1,   3, 2, 2,   3, 2, 3,

			1, 3, 1,   1, 3, 2,   1, 3, 3,
			2, 3, 1,   2, 3, 2,   2, 3, 3,
			3, 3, 1,   3, 3, 2,   3, 3, 3
		) ) );
		
		// check trapped air
		assertEquals( 0, geometry.getTrappedAir( 0 ).size() );
		assertEquals( 9, geometry.getTrappedAir( 1 ).size() );
		assertEquals( getBlocks(
			1, 1, 1,   1, 1, 2,   1, 1, 3,
			2, 1, 1,   2, 1, 2,   2, 1, 3,
			3, 1, 1,   3, 1, 2,   3, 1, 3
		), geometry.getTrappedAir( 1 ) );
		assertEquals( 18, geometry.getTrappedAir( 2 ).size() );
		assertEquals( getBlocks(
			1, 1, 1,   1, 1, 2,   1, 1, 3,
			2, 1, 1,   2, 1, 2,   2, 1, 3,
			3, 1, 1,   3, 1, 2,   3, 1, 3,
			
			1, 2, 1,   1, 2, 2,   1, 2, 3,
			2, 2, 1,   2, 2, 2,   2, 2, 3,
			3, 2, 1,   3, 2, 2,   3, 2, 3
		), geometry.getTrappedAir( 2 ) );
		assertEquals( 27, geometry.getTrappedAir( 3 ).size() );
		assertEquals( getBlocks(
			1, 1, 1,   1, 1, 2,   1, 1, 3,
			2, 1, 1,   2, 1, 2,   2, 1, 3,
			3, 1, 1,   3, 1, 2,   3, 1, 3,
			
			1, 2, 1,   1, 2, 2,   1, 2, 3,
			2, 2, 1,   2, 2, 2,   2, 2, 3,
			3, 2, 1,   3, 2, 2,   3, 2, 3,

			1, 3, 1,   1, 3, 2,   1, 3, 3,
			2, 3, 1,   2, 3, 2,   2, 3, 3,
			3, 3, 1,   3, 3, 2,   3, 3, 3
		), geometry.getTrappedAir( 3 ) );
		assertEquals( 27, geometry.getTrappedAir( 4 ).size() );
		assertEquals( getBlocks(
			1, 1, 1,   1, 1, 2,   1, 1, 3,
			2, 1, 1,   2, 1, 2,   2, 1, 3,
			3, 1, 1,   3, 1, 2,   3, 1, 3,
			
			1, 2, 1,   1, 2, 2,   1, 2, 3,
			2, 2, 1,   2, 2, 2,   2, 2, 3,
			3, 2, 1,   3, 2, 2,   3, 2, 3,

			1, 3, 1,   1, 3, 2,   1, 3, 3,
			2, 3, 1,   2, 3, 2,   2, 3, 3,
			3, 3, 1,   3, 3, 2,   3, 3, 3
		), geometry.getTrappedAir( 4 ) );
		assertEquals( 27, geometry.getTrappedAir( 5 ).size() );
		assertEquals( getBlocks(
			1, 1, 1,   1, 1, 2,   1, 1, 3,
			2, 1, 1,   2, 1, 2,   2, 1, 3,
			3, 1, 1,   3, 1, 2,   3, 1, 3,
			
			1, 2, 1,   1, 2, 2,   1, 2, 3,
			2, 2, 1,   2, 2, 2,   2, 2, 3,
			3, 2, 1,   3, 2, 2,   3, 2, 3,

			1, 3, 1,   1, 3, 2,   1, 3, 3,
			2, 3, 1,   2, 3, 2,   2, 3, 3,
			3, 3, 1,   3, 3, 2,   3, 3, 3
		), geometry.getTrappedAir( 5 ) );
	}
	
	@Test
	public void singleBlockHull( )
	{
		ShipGeometry geometry = new ShipGeometry( getBlocks(
			0, 0, 0,   0, 0, 1,   0, 0, 2,
			1, 0, 0,   1, 0, 1,   1, 0, 2,
			2, 0, 0,   2, 0, 1,   2, 0, 2,
			
			0, 1, 0,   0, 1, 1,   0, 1, 2,
			1, 1, 0,              1, 1, 2,
			2, 1, 0,   2, 1, 1,   2, 1, 2
		) );
		
		// check outer boundaries
		assertEquals( 7, geometry.getOuterBoundaries().size() );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, -1, 0,   0, -1, 1,   0, -1, 2,
			1, -1, 0,   1, -1, 1,   1, -1, 2,
			2, -1, 0,   2, -1, 1,   2, -1, 2
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
		    1, 1, 1
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, 2, 0,   0, 2, 1,   0, 2, 2,
			1, 2, 0,              1, 2, 2,
			2, 2, 0,   2, 2, 1,   2, 2, 2
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			3, 0, 0,   3, 0, 1,   3, 0, 2,
			3, 1, 0,   3, 1, 1,   3, 1, 2
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			-1, 0, 0,   -1, 0, 1,   -1, 0, 2,
			-1, 1, 0,   -1, 1, 1,   -1, 1, 2
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, 0, 3,   1, 0, 3,   2, 0, 3,
			0, 1, 3,   1, 1, 3,   2, 1, 3
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, 0, -1,   1, 0, -1,   2, 0, -1,
			0, 1, -1,   1, 1, -1,   2, 1, -1
		) ) );
		
		// check holes
		assertEquals( 0, geometry.getHoles().size() );
		
		// check trapped air
		assertEquals( 0, geometry.getTrappedAir( 0 ).size() );
		assertEquals( 1, geometry.getTrappedAir( 1 ).size() );
		assertEquals( getBlocks( 1, 1, 1 ), geometry.getTrappedAir( 1 ) );
		assertEquals( 0, geometry.getTrappedAir( 2 ).size() );
	}
	
	@Test
	public void twoLevelHull( )
	{
		ShipGeometry geometry = new ShipGeometry( getBlocks(
			0, 0, 0,   0, 0, 1,   0, 0, 2,   0, 0, 3,   0, 0, 4,
			1, 0, 0,   1, 0, 1,   1, 0, 2,   1, 0, 3,   1, 0, 4,
			2, 0, 0,   2, 0, 1,   2, 0, 2,   2, 0, 3,   2, 0, 4,
			3, 0, 0,   3, 0, 1,   3, 0, 2,   3, 0, 3,   3, 0, 4,
			
			0, 1, 0,   0, 1, 1,   0, 1, 2,   0, 1, 3,   0, 1, 4,
			1, 1, 0,                         1, 1, 3,   1, 1, 4,
			2, 1, 0,                         2, 1, 3,   2, 1, 4,
			3, 1, 0,   3, 1, 1,   3, 1, 2,   3, 1, 3,   3, 1, 4,
			
			0, 2, 0,   0, 2, 1,   0, 2, 2,   0, 2, 3,   0, 2, 4,
			1, 2, 0,                                    1, 2, 4,
			2, 2, 0,                                    2, 2, 4,
			3, 2, 0,   3, 2, 1,   3, 2, 2,   3, 2, 3,   3, 2, 4
		) );
		
		// check outer boundaries
		assertEquals( 7, geometry.getOuterBoundaries().size() );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, -1, 0,   0, -1, 1,   0, -1, 2,   0, -1, 3,   0, -1, 4,
			1, -1, 0,   1, -1, 1,   1, -1, 2,   1, -1, 3,   1, -1, 4,
			2, -1, 0,   2, -1, 1,   2, -1, 2,   2, -1, 3,   2, -1, 4,
			3, -1, 0,   3, -1, 1,   3, -1, 2,   3, -1, 3,   3, -1, 4
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, 0, -1,   1, 0, -1,   2, 0, -1,   3, 0, -1,
			0, 1, -1,   1, 1, -1,   2, 1, -1,   3, 1, -1,
			0, 2, -1,   1, 2, -1,   2, 2, -1,   3, 2, -1
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, 0, 5,   1, 0, 5,   2, 0, 5,   3, 0, 5,
			0, 1, 5,   1, 1, 5,   2, 1, 5,   3, 1, 5,
			0, 2, 5,   1, 2, 5,   2, 2, 5,   3, 2, 5
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			-1, 0, 0,   -1, 0, 1,   -1, 0, 2,   -1, 0, 3,   -1, 0, 4,
			-1, 1, 0,   -1, 1, 1,   -1, 1, 2,   -1, 1, 3,   -1, 1, 4,
			-1, 2, 0,   -1, 2, 1,   -1, 2, 2,   -1, 2, 3,   -1, 2, 4
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			4, 0, 0,   4, 0, 1,   4, 0, 2,   4, 0, 3,   4, 0, 4,
			4, 1, 0,   4, 1, 1,   4, 1, 2,   4, 1, 3,   4, 1, 4,
			4, 2, 0,   4, 2, 1,   4, 2, 2,   4, 2, 3,   4, 2, 4
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			1, 1, 1,   1, 1, 2,
			2, 1, 1,   2, 1, 2,
			
			1, 2, 1,   1, 2, 2,   1, 2, 3,
			2, 2, 1,   2, 2, 2,   2, 2, 3
		) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks(
			0, 3, 0,   0, 3, 1,   0, 3, 2,   0, 3, 3,   0, 3, 4,
			1, 3, 0,                                    1, 3, 4,
			2, 3, 0,                                    2, 3, 4,
			3, 3, 0,   3, 3, 1,   3, 3, 2,   3, 3, 3,   3, 3, 4
		) ) );
		
		// check holes
		assertEquals( 0, geometry.getHoles().size() );
		
		// check trapped air
		assertEquals( 0, geometry.getTrappedAir( 0 ).size() );
		assertEquals( 4, geometry.getTrappedAir( 1 ).size() );
		assertEquals( getBlocks(
			1, 1, 1,   1, 1, 2,
			2, 1, 1,   2, 1, 2
		), geometry.getTrappedAir( 1 ) );
		assertEquals( 10, geometry.getTrappedAir( 2 ).size() );
		assertEquals( getBlocks(
			1, 1, 1,   1, 1, 2,
			2, 1, 1,   2, 1, 2,
			
			1, 2, 1,   1, 2, 2,   1, 2, 3,
			2, 2, 1,   2, 2, 2,   2, 2, 3
		), geometry.getTrappedAir( 2 ) );
		assertEquals( 0, geometry.getTrappedAir( 3 ).size() );
	}
	
	@Test
	public void singleBlockHullEdgeNeighbors( )
	{
		ShipGeometry geometry = new ShipGeometry( getBlocks(
			
			           1, 0, 1,
			
			           0, 1, 1,
			1, 1, 0,              1, 1, 2,
			           2, 1, 1
		) );
		
		// check outer boundaries
		assertEquals( 18, geometry.getOuterBoundaries().size() );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 1, -1, 1 ) ) );
		
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 0, 0, 1 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 1, 0, 0 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 1, 0, 2 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 2, 0, 1 ) ) );
		
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( -1, 1, 1 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 0, 1, 0 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 0, 1, 2 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 1, 1, -1 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 1, 1, 1 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 1, 1, 3 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 2, 1, 0 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 2, 1, 2 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 3, 1, 1 ) ) );
		
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 0, 2, 1 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 1, 2, 0 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 1, 2, 2 ) ) );
		assertTrue( geometry.getOuterBoundaries().contains( getBlocks( 2, 2, 1 ) ) );
		
		// check holes
		assertEquals( 0, geometry.getHoles().size() );
		
		// check trapped air
		assertEquals( 0, geometry.getTrappedAir( 0 ).size() );
		assertEquals( 1, geometry.getTrappedAir( 1 ).size() );
		assertEquals( getBlocks( 1, 1, 1 ), geometry.getTrappedAir( 1 ) );
		assertEquals( 0, geometry.getTrappedAir( 2 ).size() );
	}
	
	private TreeSet<ChunkCoordinates> getBlocks( int ... coords )
	{
		if( coords.length % 3 != 0 )
		{
			throw new IllegalArgumentException( "Need coordinates in multiples of three!" );
		}
		
		TreeSet<ChunkCoordinates> blocks = new TreeSet<ChunkCoordinates>();
		int numCoords = coords.length/3;
		for( int i=0; i<numCoords; i++ )
		{
			blocks.add( new ChunkCoordinates( coords[i*3+0], coords[i*3+1], coords[i*3+2] ) );
		}
		return blocks;
	}
}
