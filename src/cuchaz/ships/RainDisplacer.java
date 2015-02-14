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
import net.minecraft.init.Blocks;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.Coords;

public class RainDisplacer extends Displacer {
	
	public RainDisplacer(EntityShip ship) {
		super(ship, Ships.m_blockAirRoof);
	}
	
	public void update() {
		// use the top envelope of the ship
		BlockSet topEnvelope = m_ship.getShipWorld().getGeometry().getEnvelopes().getEnvelope(BlockSide.Top).toBlockSet();
		m_shouldBeDisplaced.clear();
		m_ship.getCollider().getIntersectingWorldBlocks(m_shouldBeDisplaced, topEnvelope, -0.5, true);
		
		// filter out blocks that aren't air
		Iterator<Coords> iter = m_shouldBeDisplaced.iterator();
		while (iter.hasNext()) {
			Coords coords = iter.next();
			Block block = m_ship.worldObj.getBlock(coords.x, coords.y, coords.z);
			if (block != Blocks.air && block != m_block) {
				iter.remove();
			}
		}
		
		updateDisplacement();
	}
}
