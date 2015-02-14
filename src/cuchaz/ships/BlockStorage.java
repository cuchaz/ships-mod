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

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.UpdateRules;
import cuchaz.modsShared.blocks.Coords;

public class BlockStorage {
	
	public Block block;
	public int meta;
	
	public BlockStorage() {
		this(Blocks.air, 0);
	}
	
	public BlockStorage(Block block, int meta) {
		this.block = block;
		this.meta = meta;
	}
	
	public void readFromWorld(World world, Coords coords) {
		block = world.getBlock(coords.x, coords.y, coords.z);
		meta = world.getBlockMetadata(coords.x, coords.y, coords.z);
	}
	
	public void writeToWorld(World world, Coords coords) {
		BlockUtils.changeBlockWithoutNotifyingIt(world, coords.x, coords.y, coords.z, block, meta, UpdateRules.UpdateClients);
	}
}
