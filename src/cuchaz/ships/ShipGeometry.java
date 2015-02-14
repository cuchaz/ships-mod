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

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.modsShared.blocks.Envelopes;
import cuchaz.modsShared.math.BoxCorner;
import cuchaz.modsShared.math.RotatedBB;

public class ShipGeometry {
	
	private BlockSet m_blocks;
	private Envelopes m_envelopes;
	
	public ShipGeometry(BlockSet blocks) {
		m_blocks = blocks;
		
		m_envelopes = new Envelopes(m_blocks);
	}
	
	public BlockSet getBlocks() {
		return m_blocks;
	}
	
	public Envelopes getEnvelopes() {
		return m_envelopes;
	}
	
	public BlockSet rangeQuery(RotatedBB box) {
		// get the bounds in y
		int minY = MathHelper.floor_double(box.getMinY());
		int maxY = MathHelper.floor_double(box.getMaxY());
		
		BlockSet blocks = new BlockSet();
		for (int y = minY; y <= maxY; y++) {
			// add up the blocks from the xz range query
			blocks.addAll(xzRangeQuery(y, box));
		}
		return blocks;
	}
	
	public BlockSet xzRangeQuery(int y, RotatedBB box) {
		// UNDONE: we can probably optimize this using a better algorithm
		
		Vec3 p = Vec3.createVectorHelper(0, 0, 0);
		
		// get the bounds in x and z
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (BoxCorner corner : BlockSide.Top.getCorners()) {
			box.getCorner(p, corner);
			int x = MathHelper.floor_double(p.xCoord);
			int z = MathHelper.floor_double(p.zCoord);
			
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
		}
		
		// search over the blocks in the range
		Coords coords = new Coords();
		BlockSet blocks = new BlockSet();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				coords.set(x, y, z);
				
				// is there even a block here?
				if (m_blocks.contains(coords)) {
					continue;
				}
				
				if (blockIntersectsBoxXZ(x, z, box)) {
					blocks.add(new Coords(coords));
				}
			}
		}
		return blocks;
	}
	
	public BlockSet rangeQuery(AxisAlignedBB box) {
		// get the block coordinate bounds for y
		int minY = MathHelper.floor_double(box.minY);
		int maxY = MathHelper.floor_double(box.maxY);
		
		BlockSet blocks = new BlockSet();
		for (int y = minY; y <= maxY; y++) {
			blocks.addAll(rangeQuery(box, y));
		}
		return blocks;
	}
	
	public BlockSet rangeQuery(AxisAlignedBB box, int y) {
		// get the block coordinate bounds for x and z
		int minX = MathHelper.floor_double(box.minX);
		int minZ = MathHelper.floor_double(box.minZ);
		int maxX = MathHelper.floor_double(box.maxX);
		int maxZ = MathHelper.floor_double(box.maxZ);
		
		Coords coords = new Coords();
		BlockSet blocks = new BlockSet();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				coords.set(x, y, z);
				if (m_blocks.contains(coords)) {
					blocks.add(new Coords(coords));
				}
			}
		}
		return blocks;
	}
	
	private boolean blockIntersectsBoxXZ(int x, int z, RotatedBB box) {
		// return true if any xz corner of the block is in the rotated box
		double y = (box.getMinY() + box.getMaxY()) / 2;
		return box.containsPoint(x + 0, y, z + 0)
			|| box.containsPoint(x + 0, y, z + 1)
			|| box.containsPoint(x + 1, y, z + 0)
			|| box.containsPoint(x + 1, y, z + 1)
			|| anyCornerIsInBlockXZ(box, x, z);
	}
	
	private boolean anyCornerIsInBlockXZ(RotatedBB box, int x, int z) {
		Vec3 p = Vec3.createVectorHelper(0, 0, 0);
		for (BoxCorner corner : BlockSide.Top.getCorners()) {
			box.getCorner(p, corner);
			if (isPointInBlockXZ(p.xCoord, p.zCoord, x, z)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isPointInBlockXZ(double px, double pz, int blockX, int blockZ) {
		return px >= blockX && px <= blockX + 1 && pz >= blockZ && pz <= blockZ + 1;
	}
}
