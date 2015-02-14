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

import java.util.Map;

import net.minecraft.world.World;
import cuchaz.modsShared.blocks.BlockMap;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BoundingBoxInt;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.config.BlockProperties;

public class BlocksStorage {
	
	private static final Coords Origin = new Coords(0, 0, 0);
	
	private BlockMap<BlockStorage> m_blocks;
	private final BlockStorage m_airBlockStorage;
	private ShipGeometry m_geometry;
	private ShipDisplacement m_displacement;
	
	public BlocksStorage() {
		m_blocks = new BlockMap<BlockStorage>();
		m_airBlockStorage = new BlockStorage();
		m_geometry = null;
		m_displacement = null;
	}
	
	public void clear() {
		m_blocks.clear();
		m_geometry = null;
		m_displacement = null;
	}
	
	public void readFromWorld(World world, Coords originCoords, BlockSet blocks) {
		clear();
		
		// copy the blocks into storage
		for (Coords worldCoords : blocks) {
			BlockStorage storage = new BlockStorage();
			storage.readFromWorld(world, worldCoords);
			
			// make all the blocks relative to the origin block
			Coords relativeCoords = new Coords(worldCoords.x - originCoords.x, worldCoords.y - originCoords.y, worldCoords.z - originCoords.z);
			m_blocks.put(relativeCoords, storage);
		}
	}
	
	public void writeToWorld(World world, Map<Coords,Coords> correspondence) {
		// copy the blocks to the world
		for (Map.Entry<Coords,BlockStorage> entry : m_blocks.entrySet()) {
			Coords coordsShip = entry.getKey();
			Coords coordsWorld = correspondence.get(coordsShip);
			BlockStorage storage = entry.getValue();
			storage.writeToWorld(world, coordsWorld);
		}
	}
	
	public String dumpBlocks() {
		StringBuilder buf = new StringBuilder();
		for (Map.Entry<Coords,BlockStorage> entry : m_blocks.entrySet()) {
			Coords coords = entry.getKey();
			BlockStorage storage = entry.getValue();
			
			buf.append(String.format("%3d,%3d,%3d %s %4d\n", coords.x, coords.y, coords.z, storage.block.getUnlocalizedName(), storage.meta));
		}
		return buf.toString();
	}
	
	public ShipGeometry getGeometry() {
		if (m_geometry == null) {
			m_geometry = new ShipGeometry(new BlockSet(m_blocks.keySet()));
		}
		return m_geometry;
	}
	
	public ShipDisplacement getDisplacement() {
		if (m_displacement == null) {
			BlockSet watertightBlocks = new BlockSet();
			for (Coords coords : m_blocks.keySet()) {
				if (BlockProperties.isWatertight(getBlock(coords).block)) {
					watertightBlocks.add(coords);
				}
			}
			m_displacement = new ShipDisplacement(watertightBlocks);
		}
		return m_displacement;
	}
	
	public int getNumBlocks() {
		return m_blocks.size();
	}
	
	public BlockSet coords() {
		return m_blocks.blockSet();
	}
	
	public BlockStorage getBlock(Coords coords) {
		BlockStorage storage = m_blocks.get(coords);
		if (storage == null) {
			storage = m_airBlockStorage;
		}
		return storage;
	}
	
	public void setBlock(Coords coords, BlockStorage val) {
		m_blocks.put(coords, val);
	}
	
	public BoundingBoxInt getBoundingBox() {
		return getGeometry().getEnvelopes().getBoundingBox();
	}
	
	public ShipType getShipType() {
		return ShipType.getByMeta(getShipBlock().meta);
	}
	
	public BlockStorage getShipBlock() {
		BlockStorage block = m_blocks.get(Origin);
		if (block == null) {
			throw new ShipConfigurationException("Ship does not have a ship block!");
		}
		if (block.block != Ships.m_blockShip) {
			throw new ShipConfigurationException("Ship origin block is not a ship block!");
		}
		return block;
	}
}
