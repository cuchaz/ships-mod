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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.Neighbors;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.BlocksStorage;

public class SailDiscoverer implements PropulsionDiscoverer {
	
	@Override
	public List<PropulsionMethod> getPropulsionMethods(BlocksStorage shipBlocks, BlockSide frontDirection) {
		// collect all the cloth blocks into connected components
		BlockSet clothCoords = new BlockSet();
		for (Coords coords : shipBlocks.coords()) {
			if (shipBlocks.getBlock(coords).block == Blocks.wool) {
				clothCoords.add(coords);
			}
		}
		List<BlockSet> clothComponents = BlockUtils.getConnectedComponents(clothCoords, Neighbors.Edges);
		
		// build the sails
		List<PropulsionMethod> sails = new ArrayList<PropulsionMethod>();
		for (BlockSet component : clothComponents) {
			Sail sail = new Sail(shipBlocks, component, frontDirection);
			if (sail.isValid()) {
				sails.add(sail);
			}
		}
		return sails;
	}
}
