/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import java.util.Iterator;

import net.minecraft.block.Block;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.config.BlockProperties;

public class WaterDisplacer extends Displacer {
	
	public WaterDisplacer(EntityShip ship) {
		super(ship, Ships.m_blockAirWall);
	}
	
	public void update(double waterHeightInBlockSpace) {
		// get all the trapped air blocks
		BlockSet trappedAirBlocks = m_ship.getShipWorld().getDisplacement().getTrappedAirFromWaterHeight(waterHeightInBlockSpace);
		if (trappedAirBlocks.isEmpty()) {
			// the ship is out of the water or flooded
			return;
		}
		
		// translate to world blocks
		m_shouldBeDisplaced.clear();
		m_ship.getCollider().getIntersectingWorldBlocks(m_shouldBeDisplaced, trappedAirBlocks, 0.01, false);
		
		// filter out blocks that aren't water
		Iterator<Coords> iter = m_shouldBeDisplaced.iterator();
		while (iter.hasNext()) {
			Coords coords = iter.next();
			Block block = m_ship.worldObj.getBlock(coords.x, coords.y, coords.z);
			if (!BlockProperties.isWater(block)) {
				iter.remove();
			}
		}
		
		updateDisplacement();
	}
}
