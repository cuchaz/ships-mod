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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockUtils;

public class TestShipGeometry
{
	private static final BlockSet EmptyBlocks = new BlockSet();
	
	@Test
	public void singleBlockOuterBoudaries( )
	{
		ShipGeometry geometry = getShip( 3,
			" . ",
			"...",
			" . ",
			
			"...",
			".x.",
			"...",
			
			" . ",
			"...",
			" . "
		);
		
		// check trapped air
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 0 ) );
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 1 ) );
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 2 ) );
	}
	
	@Test
	public void singleBlockHole( )
	{
		ShipGeometry geometry = getShip( 5,
			" ... ",
			".....",
			".....",
			".....",
			" ... ",
			
			".....",
			".xxx.",
			".xxx.",
			".xxx.",
			".....",
			
			".....",
			".xxx.",
			".x-x.",
			".xxx.",
			".....",
			
			".....",
			".xxx.",
			".xxx.",
			".xxx.",
			".....",
			
			" ... ",
			".....",
			".....",
			".....",
			" ... "
		);
		
		// check trapped air
		BlockSet hole = new BlockSet( 1, 2, 2, 2,
			"x"
		);
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 0 ) );
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 1 ) );
		assertEquals( hole, geometry.getTrappedAir( 2 ) );
		assertEquals( hole, geometry.getTrappedAir( 3 ) );
		assertEquals( hole, geometry.getTrappedAir( 4 ) );
	}
	
	@Test
	public void threeByThreeByThreeHole( )
	{
		ShipGeometry geometry = getShip( 7,
			" ..... ",
			".......",
			".......",
			".......",
			".......",
			".......",
			" ..... ",
			
			".......",
			".xxxxx.",
			".xxxxx.",
			".xxxxx.",
			".xxxxx.",
			".xxxxx.",
			".......",
			
			".......",
			".xxxxx.",
			".x---x.",
			".x---x.",
			".x---x.",
			".xxxxx.",
			".......",
			
			".......",
			".xxxxx.",
			".x---x.",
			".x---x.",
			".x---x.",
			".xxxxx.",
			".......",
			
			".......",
			".xxxxx.",
			".x---x.",
			".x---x.",
			".x---x.",
			".xxxxx.",
			".......",
			
			".......",
			".xxxxx.",
			".xxxxx.",
			".xxxxx.",
			".xxxxx.",
			".xxxxx.",
			".......",
			
			" ..... ",
			".......",
			".......",
			".......",
			".......",
			".......",
			" ..... "
		);
		
		// check trapped air
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 0 ) );
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 1 ) );
		assertEquals( new BlockSet( 3, 2, 2, 2,
			"xxx",
			"xxx",
			"xxx"
		), geometry.getTrappedAir( 2 ) );
		assertEquals( new BlockSet( 3, 2, 2, 2,
			"xxx",
			"xxx",
			"xxx",
			
			"xxx",
			"xxx",
			"xxx"
		), geometry.getTrappedAir( 3 ) );
		BlockSet hole = new BlockSet( 3, 2, 2, 2,
			"xxx",
			"xxx",
			"xxx",
			
			"xxx",
			"xxx",
			"xxx",
			
			"xxx",
			"xxx",
			"xxx"
		);
		assertEquals( hole, geometry.getTrappedAir( 4 ) );
		assertEquals( hole, geometry.getTrappedAir( 5 ) );
		assertEquals( hole, geometry.getTrappedAir( 6 ) );
	}
	
	@Test
	public void singleBlockHull( )
	{
		ShipGeometry geometry = getShip( 5,
			" ... ",
			".....",
			".....",
			".....",
			" ... ",
			
			".....",
			".xxx.",
			".xxx.",
			".xxx.",
			".....",
			
			".....",
			".xxx.",
			".x.x.",
			".xxx.",
			".....",
			
			" ... ",
			".....",
			".....",
			".....",
			" ... "
		);
		
		// check trapped air
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 0 ) );
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 1 ) );
		BlockSet hole = new BlockSet( 1, 2, 2, 2,
			"x"
		);
		assertEquals( hole, geometry.getTrappedAir( 2 ) );
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 3 ) );
	}
	
	@Test
	public void twoLevelHull( )
	{
		ShipGeometry geometry = getShip( 6,
			" ..... ",
			".......",
			".......",
			".......",
			".......",
			" ..... ",
			
			".......",
			".xxxxx.",
			".xxxxx.",
			".xxxxx.",
			".xxxxx.",
			".......",
			
			".......",
			".xxxxx.",
			".x..xx.",
			".x..xx.",
			".xxxxx.",
			".......",
			
			".......",
			".xxxxx.",
			".x...x.",
			".x...x.",
			".xxxxx.",
			".......",
			
			" ..... ",
			".......",
			".......",
			".......",
			".......",
			" ..... "
		);
		
		// check trapped air
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 0 ) );
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 1 ) );
		assertEquals( new BlockSet( 2, 2, 2, 2,
			"xx",
			"xx"
		), geometry.getTrappedAir( 2 ) );
		assertEquals( new BlockSet( 2, 2, 2, 2,
			"xx",
			"xx",
			
			"xxx",
			"xxx"
		), geometry.getTrappedAir( 3 ) );
		assertEquals( EmptyBlocks, geometry.getTrappedAir( 4 ) );
	}
	
	/*
	@Test
	public void singleBlockHullEdgeNeighbors( )
	{
		ShipGeometry geometry = new ShipGeometry( getBlocks(
			
			           2, 1, 2,
			
			           1, 2, 2,
			2, 2, 1,              2, 2, 3,
			           3, 2, 2
		) );
		
		// check outer boundaries
		assertTrue( geometry.getOuterBoundary().equals( getBlocks(
			0, 0, 0,  0, 0, 1,  0, 0, 2,  0, 0, 3,  0, 0, 4,
			1, 0, 0,  1, 0, 1,  1, 0, 2,  1, 0, 3,  1, 0, 4,
			2, 0, 0,  2, 0, 1,  2, 0, 2,  2, 0, 3,  2, 0, 4,
			3, 0, 0,  3, 0, 1,  3, 0, 2,  3, 0, 3,  3, 0, 4,
			4, 0, 0,  4, 0, 1,  4, 0, 2,  4, 0, 3,  4, 0, 4,
			
			0, 1, 0,  0, 1, 1,  0, 1, 2,  0, 1, 3,  0, 1, 4,
			1, 1, 0,  1, 1, 1,  1, 1, 2,  1, 1, 3,  1, 1, 4,
			2, 1, 0,  2, 1, 1,  2, 1, 2,  2, 1, 3,  2, 1, 4,
			3, 1, 0,  3, 1, 1,  3, 1, 2,  3, 1, 3,  3, 1, 4,
			4, 1, 0,  4, 1, 1,  4, 1, 2,  4, 1, 3,  4, 1, 4,
			
			0, 2, 0,  0, 2, 1,  0, 2, 2,  0, 2, 3,  0, 2, 4,
			1, 2, 0,  1, 2, 1,  1, 2, 2,  1, 2, 3,  1, 2, 4,
			2, 2, 0,  2, 2, 1,  2, 2, 2,  2, 2, 3,  2, 2, 4,
			3, 2, 0,  3, 2, 1,  3, 2, 2,  3, 2, 3,  3, 2, 4,
			4, 2, 0,  4, 2, 1,  4, 2, 2,  4, 2, 3,  4, 2, 4,
			
			0, 3, 0,  0, 3, 1,  0, 3, 2,  0, 3, 3,  0, 3, 4,
			1, 3, 0,  1, 3, 1,  1, 3, 2,  1, 3, 3,  1, 3, 4,
			2, 3, 0,  2, 3, 1,  2, 3, 2,  2, 3, 3,  2, 3, 4,
			3, 3, 0,  3, 3, 1,  3, 3, 2,  3, 3, 3,  3, 3, 4,
			4, 3, 0,  4, 3, 1,  4, 3, 2,  4, 3, 3,  4, 3, 4
		) ) );
		
		// check holes
		assertEquals( 0, geometry.getHoles().size() );
		
		// check trapped air
		assertEquals( 0, geometry.getTrappedAir( 0 ).size() );
		assertEquals( 1, geometry.getTrappedAir( 1 ).size() );
		assertEquals( getBlocks( 1, 1, 1 ), geometry.getTrappedAir( 1 ) );
		assertEquals( 0, geometry.getTrappedAir( 2 ).size() );
	}
	*/
	
	private ShipGeometry getShip( int numLayers, String ... lines )
	{
		BlockSet shipBlocks = new BlockSet( numLayers, 'x', lines );
		BlockSet outerBoundary = new BlockSet( numLayers, '.', lines );
		BlockSet holeBlocks = new BlockSet( numLayers, '-', lines );
		
		// build the ship and check it
		ShipGeometry geometry = new ShipGeometry( shipBlocks );
		
		// check the outer boundary
		assertEquals( outerBoundary, geometry.getOuterBoundary() );
		
		// check the holes
		List<BlockSet> holes = BlockUtils.getConnectedComponents( holeBlocks, ShipGeometry.VoidBlockNeighbors );
		assertEquals( holes.size(), geometry.getHoles().size() );
		for( BlockSet hole : holes )
		{
			assertTrue( geometry.getHoles().contains( hole ) );
		}
		
		return geometry;
	}
}
