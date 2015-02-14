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
package cuchaz.ships.propulsion;

import net.minecraft.init.Blocks;
import cuchaz.modsShared.Util;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.BlocksStorage;

public class Sail extends PropulsionMethod {
	
	public static final double ThrustPerForwardBlock = Util.perSecond2ToPerTick2(8); // N
	public static final double ThrustPerSideBlock = Util.perSecond2ToPerTick2(2); // N
	
	private int m_numForwardBlocks;
	private int m_numSideBlocks;
	
	protected Sail(BlocksStorage shipBlocks, BlockSet sailBlocks, BlockSide frontDirection) {
		super("Sail", "Sails", sailBlocks);
		
		m_numForwardBlocks = getNumExposedBlocks(shipBlocks, sailBlocks, frontDirection);
		m_numSideBlocks = getNumExposedBlocks(shipBlocks, sailBlocks, frontDirection.rotateXZCw(1));
	}
	
	@Override
	public double getThrust(double speed) {
		// sail thrust depends on ship speed
		// sails have full strength when the ship isn't moving
		// when the ship reaches the wind speed, the sails don't add anymore thrust
		// although, wind speed is implicit here...
		return (ThrustPerForwardBlock * m_numForwardBlocks + ThrustPerSideBlock * m_numSideBlocks) / (speed + 1);
	}
	
	public boolean isValid() {
		// sails are always valid
		return true;
	}
	
	private int getNumExposedBlocks(BlocksStorage shipBlocks, BlockSet sailBlocks, BlockSide checkDirection) {
		int numExposedBlocks = 0;
		Coords checkCoords = new Coords();
		for (Coords coords : sailBlocks) {
			checkCoords.set(coords.x + checkDirection.getDx(), coords.y + checkDirection.getDy(), coords.z + checkDirection.getDz());
			if (shipBlocks.getBlock(checkCoords).block == Blocks.air) {
				numExposedBlocks++;
			}
		}
		return numExposedBlocks;
	}
}
