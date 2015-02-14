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

public class TestShipDisplacement {
	
	private static final BlockSet EmptyBlocks = new BlockSet();
	
	@Test
	public void singleBlock( ) {
		ShipDisplacement displacement = getShip(3,
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
		
		// check surface blocks
		assertEquals(0, displacement.getSurfaceBlocks(0).size());
		assertEquals(1, displacement.getSurfaceBlocks(1).size());
		assertEquals(0, displacement.getSurfaceBlocks(2).size());
		
		// check underwater blocks
		assertEquals(0, displacement.getUnderwaterBlocks(0).size());
		assertEquals(0, displacement.getUnderwaterBlocks(1).size());
		assertEquals(1, displacement.getUnderwaterBlocks(2).size());
		
		// check fillable blocks
		assertEquals(0, displacement.getNumFillableBlocks(0));
		assertEquals(0, displacement.getNumFillableBlocks(1));
		assertEquals(0, displacement.getNumFillableBlocks(2));
		
		// check trapped air
		assertEquals(EmptyBlocks, displacement.getTrappedAir(0));
		assertEquals(EmptyBlocks, displacement.getTrappedAir(1));
		assertEquals(EmptyBlocks, displacement.getTrappedAir(2));
	}
	
	@Test
	public void singleBlockHole() {
		ShipDisplacement displacement = getShip(5,
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
		
		// check surface blocks
		assertEquals(0, displacement.getSurfaceBlocks(0).size());
		assertEquals(9, displacement.getSurfaceBlocks(1).size());
		assertEquals(9, displacement.getSurfaceBlocks(2).size());
		assertEquals(9, displacement.getSurfaceBlocks(3).size());
		assertEquals(0, displacement.getSurfaceBlocks(4).size());
		
		// check underwater blocks
		assertEquals(0, displacement.getUnderwaterBlocks(0).size());
		assertEquals(0, displacement.getUnderwaterBlocks(1).size());
		assertEquals(9, displacement.getUnderwaterBlocks(2).size());
		assertEquals(18, displacement.getUnderwaterBlocks(3).size());
		assertEquals(27, displacement.getUnderwaterBlocks(4).size());
		
		// check fillable blocks
		assertEquals(0, displacement.getNumFillableBlocks(0));
		assertEquals(0, displacement.getNumFillableBlocks(1));
		assertEquals(0, displacement.getNumFillableBlocks(2));
		assertEquals(0, displacement.getNumFillableBlocks(3));
		assertEquals(0, displacement.getNumFillableBlocks(4));
		
		// check trapped air
		BlockSet hole = new BlockSet(1, 2, 2, 2,
			"x"
		);
		assertEquals(EmptyBlocks, displacement.getTrappedAir(0));
		assertEquals(EmptyBlocks, displacement.getTrappedAir(1));
		assertEquals(hole, displacement.getTrappedAir(2));
		assertEquals(hole, displacement.getTrappedAir(3));
		assertEquals(hole, displacement.getTrappedAir(4));
	}
	
	@Test
	public void threeByThreeByThreeHole() {
		ShipDisplacement displacement = getShip(7,
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
		
		// check surface blocks
		assertEquals(0, displacement.getSurfaceBlocks(0).size());
		assertEquals(25, displacement.getSurfaceBlocks(1).size());
		assertEquals(25, displacement.getSurfaceBlocks(2).size());
		assertEquals(25, displacement.getSurfaceBlocks(3).size());
		assertEquals(25, displacement.getSurfaceBlocks(4).size());
		assertEquals(25, displacement.getSurfaceBlocks(5).size());
		assertEquals(0, displacement.getSurfaceBlocks(6).size());
		
		// check underwater blocks
		assertEquals(0, displacement.getUnderwaterBlocks(0).size());
		assertEquals(0, displacement.getUnderwaterBlocks(1).size());
		assertEquals(25, displacement.getUnderwaterBlocks(2).size());
		assertEquals(50, displacement.getUnderwaterBlocks(3).size());
		assertEquals(75, displacement.getUnderwaterBlocks(4).size());
		assertEquals(100, displacement.getUnderwaterBlocks(5).size());
		assertEquals(125, displacement.getUnderwaterBlocks(6).size());
		
		// check fillable blocks
		assertEquals(0, displacement.getNumFillableBlocks(0));
		assertEquals(0, displacement.getNumFillableBlocks(1));
		assertEquals(0, displacement.getNumFillableBlocks(2));
		assertEquals(0, displacement.getNumFillableBlocks(3));
		assertEquals(0, displacement.getNumFillableBlocks(4));
		assertEquals(0, displacement.getNumFillableBlocks(5));
		assertEquals(0, displacement.getNumFillableBlocks(6));
		
		// check trapped air
		assertEquals(EmptyBlocks, displacement.getTrappedAir(0));
		assertEquals(EmptyBlocks, displacement.getTrappedAir(1));
		assertEquals(new BlockSet(3, 2, 2, 2,
			"xxx",
			"xxx",
			"xxx"
		), displacement.getTrappedAir(2));
		assertEquals(new BlockSet(3, 2, 2, 2,
			"xxx",
			"xxx",
			"xxx",
			
			"xxx",
			"xxx",
			"xxx"
		), displacement.getTrappedAir(3));
		BlockSet hole = new BlockSet(3, 2, 2, 2,
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
		assertEquals(hole, displacement.getTrappedAir(4));
		assertEquals(hole, displacement.getTrappedAir(5));
		assertEquals(hole, displacement.getTrappedAir(6));
	}
	
	@Test
	public void singleBlockHull() {
		ShipDisplacement displacement = getShip(5,
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
		
		// check surface blocks
		assertEquals(0, displacement.getSurfaceBlocks(0).size());
		assertEquals(9, displacement.getSurfaceBlocks(1).size());
		assertEquals(9, displacement.getSurfaceBlocks(2).size());
		assertEquals(0, displacement.getSurfaceBlocks(3).size());
		
		// check underwater blocks
		assertEquals(0, displacement.getUnderwaterBlocks(0).size());
		assertEquals(0, displacement.getUnderwaterBlocks(1).size());
		assertEquals(9, displacement.getUnderwaterBlocks(2).size());
		assertEquals(17, displacement.getUnderwaterBlocks(3).size());
		
		// check fillable blocks
		assertEquals(0, displacement.getNumFillableBlocks(0));
		assertEquals(0, displacement.getNumFillableBlocks(1));
		assertEquals(1, displacement.getNumFillableBlocks(2));
		assertEquals(0, displacement.getNumFillableBlocks(3));
		
		// check trapped air
		assertEquals(EmptyBlocks, displacement.getTrappedAir(0));
		assertEquals(EmptyBlocks, displacement.getTrappedAir(1));
		BlockSet hole = new BlockSet(1, 2, 2, 2,
			"x"
		);
		assertEquals(hole, displacement.getTrappedAir(2));
		assertEquals(EmptyBlocks, displacement.getTrappedAir(3));
	}
	
	@Test
	public void twoLevelHull() {
		ShipDisplacement displacement = getShip(6,
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
		
		// check surface blocks
		assertEquals(0, displacement.getSurfaceBlocks(0).size());
		assertEquals(20, displacement.getSurfaceBlocks(1).size());
		assertEquals(20, displacement.getSurfaceBlocks(2).size());
		assertEquals(20, displacement.getSurfaceBlocks(3).size());
		assertEquals(0, displacement.getSurfaceBlocks(4).size());
		
		// check underwater blocks
		assertEquals(0, displacement.getUnderwaterBlocks(0).size());
		assertEquals(0, displacement.getUnderwaterBlocks(1).size());
		assertEquals(20, displacement.getUnderwaterBlocks(2).size());
		assertEquals(40, displacement.getUnderwaterBlocks(3).size());
		assertEquals(50, displacement.getUnderwaterBlocks(4).size());
		
		// check fillable blocks
		assertEquals(0, displacement.getNumFillableBlocks(0));
		assertEquals(0, displacement.getNumFillableBlocks(1));
		assertEquals(0, displacement.getNumFillableBlocks(2));
		assertEquals(10, displacement.getNumFillableBlocks(3));
		assertEquals(0, displacement.getNumFillableBlocks(4));
		
		// check trapped air
		assertEquals(EmptyBlocks, displacement.getTrappedAir(0));
		assertEquals(EmptyBlocks, displacement.getTrappedAir(1));
		assertEquals(new BlockSet(2, 2, 2, 2,
			"xx",
			"xx"
		), displacement.getTrappedAir(2));
		assertEquals(new BlockSet(2, 2, 2, 2,
			"xx",
			"xx",
			
			"xxx",
			"xxx"
		), displacement.getTrappedAir(3));
		assertEquals(EmptyBlocks, displacement.getTrappedAir(4));
	}
	
	@Test
	public void singleBlockHullEdgeNeighbors() {
		ShipDisplacement displacement = getShip(5,
			"     ",
			"  .  ",
			" ... ",
			"  .  ",
			"     ",
			
			"  .  ",
			" ... ",
			"..x..",
			" ... ",
			"  .  ",
			
			" ... ",
			"..x..",
			".x.x.",
			"..x..",
			" ... ",
			
			"  .  ",
			" ... ",
			".....",
			" ... ",
			"  .  "
		);
		
		// check surface blocks
		assertEquals(0, displacement.getSurfaceBlocks(0).size());
		assertEquals(1, displacement.getSurfaceBlocks(1).size());
		assertEquals(5, displacement.getSurfaceBlocks(2).size());
		assertEquals(0, displacement.getSurfaceBlocks(3).size());
		
		// check underwater blocks
		assertEquals(0, displacement.getUnderwaterBlocks(0).size());
		assertEquals(0, displacement.getUnderwaterBlocks(1).size());
		assertEquals(1, displacement.getUnderwaterBlocks(2).size());
		assertEquals(5, displacement.getUnderwaterBlocks(3).size());
		
		// check fillable blocks
		assertEquals(0, displacement.getNumFillableBlocks(0));
		assertEquals(0, displacement.getNumFillableBlocks(1));
		assertEquals(1, displacement.getNumFillableBlocks(2));
		assertEquals(0, displacement.getNumFillableBlocks(3));
		
		// check trapped air
		BlockSet hole = new BlockSet(1, 2, 2, 2,
			"x"
		);
		assertEquals(EmptyBlocks, displacement.getTrappedAir(0));
		assertEquals(EmptyBlocks, displacement.getTrappedAir(1));
		assertEquals(hole, displacement.getTrappedAir(2));
		assertEquals(EmptyBlocks, displacement.getTrappedAir(3));
	}
	
	private ShipDisplacement getShip(int numLayers, String... lines) {
		BlockSet shipBlocks = new BlockSet(numLayers, 'x', lines);
		BlockSet outerBoundary = new BlockSet(numLayers, '.', lines);
		BlockSet holeBlocks = new BlockSet(numLayers, '-', lines);
		
		// build the ship and check it
		ShipDisplacement displacement = new ShipDisplacement(shipBlocks);
		
		// check the outer boundary
		BlockSet outerBoundaryObserved = new BlockSet();
		for (BlockSet blocks : displacement.getOuterBoundaries()) {
			outerBoundaryObserved.addAll(blocks);
		}
		assertEquals(outerBoundary, outerBoundaryObserved);
		
		// check the holes
		List<BlockSet> holes = BlockUtils.getConnectedComponents(holeBlocks, ShipDisplacement.VoidBlockNeighbors);
		assertEquals(holes.size(), displacement.getHoles().size());
		for (BlockSet hole : holes) {
			assertTrue(displacement.getHoles().contains(hole));
		}
		
		return displacement;
	}
}
