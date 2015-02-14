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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.block.Block;
import cuchaz.modsShared.blocks.BlockMap;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.UpdateRules;
import cuchaz.modsShared.blocks.Coords;

public abstract class Displacer {
	
	protected EntityShip m_ship;
	protected Block m_block;
	private BlockMap<Integer> m_displacedBlocks;
	protected BlockSet m_shouldBeDisplaced;
	
	protected Displacer(EntityShip ship, Block block) {
		m_ship = ship;
		m_block = block;
		m_displacedBlocks = new BlockMap<Integer>();
		m_shouldBeDisplaced = new BlockSet();
	}
	
	protected void updateDisplacement() {
		// are there any blocks that are displaced, but shouldn't be?
		Iterator<Map.Entry<Coords,Integer>> iter = m_displacedBlocks.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Coords,Integer> entry = iter.next();
			Coords coords = entry.getKey();
			if (!m_shouldBeDisplaced.contains(coords)) {
				restoreBlock(coords, entry.getValue());
				iter.remove();
			}
		}
		
		// make sure that all the blocks that should be displaced are actually displaced
		for (Coords coords : m_shouldBeDisplaced) {
			Block block = m_ship.worldObj.getBlock(coords.x, coords.y, coords.z);
			int blockMeta = m_ship.worldObj.getBlockMetadata(coords.x, coords.y, coords.z);
			if (block != m_block) {
				// displace the block
				BlockUtils.changeBlockWithoutNotifyingIt(m_ship.worldObj, coords.x, coords.y, coords.z, m_block, 0, UpdateRules.UpdateClients);
				
				// remember that we displaced it
				m_displacedBlocks.put(coords, pack(block, blockMeta));
			} else {
				// we sailed over a block that has already been displaced
				// make sure we remember it, but don't erase the displaced block info
				if (!m_displacedBlocks.containsKey(coords)) {
					m_displacedBlocks.put(coords, pack(block, blockMeta));
				}
			}
		}
	}
	
	public void restore() {
		for (Map.Entry<Coords,Integer> entry : m_displacedBlocks.entrySet()) {
			restoreBlock(entry.getKey(), entry.getValue());
		}
		m_displacedBlocks.clear();
	}
	
	public byte[] write() {
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			GZIPOutputStream zipOut = new GZIPOutputStream(buf);
			DataOutputStream out = new DataOutputStream(zipOut);
			
			out.writeInt(m_displacedBlocks.size());
			for (Map.Entry<Coords,Integer> entry : m_displacedBlocks.entrySet()) {
				Coords coords = entry.getKey();
				int packed = entry.getValue();
				out.writeInt(coords.x);
				out.writeInt(coords.y);
				out.writeInt(coords.z);
				out.writeInt(packed);
			}
			
			zipOut.finish();
			out.close();
			return buf.toByteArray();
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}
	
	public void read(byte[] data) {
		try {
			DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data)));
			
			int numEntries = in.readInt();
			for (int i = 0; i < numEntries; i++) {
				m_displacedBlocks.put(new Coords(in.readInt(), in.readInt(), in.readInt()), in.readInt());
			}
			
			in.close();
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}
	
	private void restoreBlock(Coords coords, int packed) {
		if (m_ship.worldObj.getBlock(coords.x, coords.y, coords.z) == m_block) {
			Block block = unpackId(packed);
			int blockMeta = unpackMeta(packed);
			BlockUtils.changeBlockWithoutNotifyingIt(m_ship.worldObj, coords.x, coords.y, coords.z, block, blockMeta, UpdateRules.UpdateClients);
		}
	}
	
	private int pack(Block block, int meta) {
		return Block.getIdFromBlock(block) | ( (meta & 0xf) << 16);
	}
	
	private Block unpackId(int packed) {
		return Block.getBlockById(packed & 0xfff);
	}
	
	private int unpackMeta(int packed) {
		return (packed >> 16) & 0xf;
	}
}
