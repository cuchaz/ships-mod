/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.minecraft.block.Block;

import org.junit.Test;

import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.Neighbors;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.config.BlockProperties;
import cuchaz.ships.persistence.BlockStoragePersistence;

public class TestDisplacementComponents {
	
	// @Test
	public void shipA() throws Exception {
		new MinecraftRunner() {
			
			@Override
			public void onRun() throws Exception {
				testShip("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");
			}
		}.run();
	}
	
	// @Test
	public void shipB() throws Exception {
		new MinecraftRunner() {
			
			@Override
			public void onRun() throws Exception {
				testShip("H4sIAAAAAAAAAD2SO3IEMQhENVO1Z/J9HDja+ycI2gge2qQX1B+gRlKstf4UMu31WfX7/kpdJ25ZYlwMeaGn8rwrBZ/i97td/uj3+ildoPPWpfLUjTr/8J9++zu1377jK3Kmv5lDzCFyjHdD5zXPZg8jx+58m3lm39kn0EfqD3/0zpyGfnR1z/ZdD/herJzs112T1/iCT/uW3tE5vLxn6Q39qR/qt++evKlFTpAT9AfrTpUn8oRueK0Xet1cMY/IFXxHP3u2v8P3ywt4s9/cazPPxtfJc/YOcr32DnwH63ss/86d2u5djZy51/gF/pPXtcZn/A/+A3s5wZQ4AwAA");
			}
		}.run();
	}
	
	private void testShip(String encodedBlocks) throws Exception {
		// load the ship blocks
		BlocksStorage shipBlocks = BlockStoragePersistence.readAnyVersion(encodedBlocks);
		int minY = shipBlocks.getBoundingBox().minY;
		int maxY = shipBlocks.getBoundingBox().maxY;
		ShipDisplacement displacement = shipBlocks.getDisplacement();
		
		// check all displaced blocks
		BlockSet displacedBlocks = new BlockSet();
		displacedBlocks.addAll(displacement.getSurfaceBlocks(maxY));
		displacedBlocks.addAll(displacement.getUnderwaterBlocks(maxY));
		testBlocks(displacedBlocks);
		
		// check the trapped air by itself
		displacedBlocks.clear();
		displacedBlocks.addAll(displacement.getTrappedAirFromWaterHeight(maxY));
		testBlocks(displacedBlocks);
		
		// sweep over the trapped air
		for (int y = minY; y <= maxY + 1; y++) {
			int waterHeight = y + 1;
			BlockSet trappedAir = displacement.getTrappedAirFromWaterHeight(waterHeight);
			
			for (Coords coords : trappedAir) {
				// make sure the block is not watertight
				Block block = shipBlocks.getBlock(coords).block;
				assertFalse(BlockProperties.isWatertight(block));
				
				// this block should not be connected to the shell below this y
				assertFalse(BlockUtils.isConnectedToShell(coords, shipBlocks.coords(), Neighbors.Faces, y));
			}
			
			BlockSet boundary = BlockUtils.getBoundary(trappedAir, Neighbors.Faces);
			for (Coords coords : boundary) {
				// skip anything above the y cap
				if (coords.y > y) {
					continue;
				}
				
				// this block should be a watertight block
				Block block = shipBlocks.getBlock(coords).block;
				assertTrue(String.format("Expected a watertight block, but got %s at (%d,%d,%d) at maxY=%d instead!\nTrapped air %s:\n%s\n", block == null ? "Air" : block.getUnlocalizedName(), coords.x, coords.y, coords.z, y, trappedAir.getBoundingBox().toString(), trappedAir.toString()), BlockProperties.isWatertight(block));
			}
		}
	}
	
	public void testBlocks(BlockSet blocks) {
		// there should be one connected component
		assertEquals(1, BlockUtils.getConnectedComponents(blocks, Neighbors.Edges).size());
		
		// the boundary of this set should also have one connected component
		// ie, there should be no holes in the block set
		assertEquals(1, BlockUtils.getConnectedComponents(BlockUtils.getBoundary(blocks, Neighbors.Edges), Neighbors.Edges).size());
	}
}
