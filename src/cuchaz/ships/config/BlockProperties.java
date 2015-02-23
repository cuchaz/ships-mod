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
package cuchaz.ships.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import cuchaz.ships.Ships;

public class BlockProperties {
	
	private static Map<Block,BlockEntry> m_hardcodedEntries;
	private static Map<Block,BlockEntry> m_overriddenEntries;
	
	static {
		m_hardcodedEntries = new HashMap<Block,BlockEntry>();
		m_overriddenEntries = new HashMap<Block,BlockEntry>();
		
		// add some hard-coded entries for vanilla blocks that have weird shapes, but common materials
		final double DoorSizeFactor = 0.3;
		addScaledEntry(Blocks.wooden_door, new BlockEntry(DoorSizeFactor, DoorSizeFactor, false, false, false));
		addScaledEntry(Blocks.iron_door, new BlockEntry(DoorSizeFactor, DoorSizeFactor, false, false, false));
		
		final double TrapDoorSizeFactor = 0.2;
		addScaledEntry(Blocks.trapdoor, new BlockEntry(TrapDoorSizeFactor, TrapDoorSizeFactor, false, false, false));
		
		final double SlabSizeFactor = 0.5;
		addScaledEntry(Blocks.stone_slab, new BlockEntry(SlabSizeFactor, SlabSizeFactor, true, false, false));
		addScaledEntry(Blocks.wooden_slab, new BlockEntry(SlabSizeFactor, SlabSizeFactor, true, false, false));
		
		final double StairsSizeFactor = 0.75;
		addScaledEntry(Blocks.oak_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		addScaledEntry(Blocks.spruce_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		addScaledEntry(Blocks.birch_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		addScaledEntry(Blocks.jungle_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		addScaledEntry(Blocks.quartz_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		addScaledEntry(Blocks.brick_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		addScaledEntry(Blocks.stone_brick_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		addScaledEntry(Blocks.nether_brick_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		addScaledEntry(Blocks.sandstone_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		addScaledEntry(Blocks.acacia_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		addScaledEntry(Blocks.dark_oak_stairs, new BlockEntry(StairsSizeFactor, StairsSizeFactor, true, false, false));
		
		final double LadderSizeFactor = 0.1;
		addScaledEntry(Blocks.ladder, new BlockEntry(LadderSizeFactor, LadderSizeFactor, false, false, false));
		
		final double FenceSizeFactor = 0.5;
		addScaledEntry(Blocks.fence, new BlockEntry(FenceSizeFactor, FenceSizeFactor, false, false, false));
		addScaledEntry(Blocks.fence_gate, new BlockEntry(FenceSizeFactor, FenceSizeFactor, false, false, false));
		addScaledEntry(Blocks.nether_brick_fence, new BlockEntry(FenceSizeFactor, FenceSizeFactor, false, false, false));
		addScaledEntry(Blocks.iron_bars, new BlockEntry(FenceSizeFactor, FenceSizeFactor, false, false, false));
		
		final double PressurePlateFactor = 0.2;
		addScaledEntry(Blocks.stone_pressure_plate, new BlockEntry(PressurePlateFactor, PressurePlateFactor, false, false, false));
		addScaledEntry(Blocks.wooden_pressure_plate, new BlockEntry(PressurePlateFactor, PressurePlateFactor, false, false, false));
		addScaledEntry(Blocks.light_weighted_pressure_plate, new BlockEntry(PressurePlateFactor, PressurePlateFactor, false, false, false));
		addScaledEntry(Blocks.heavy_weighted_pressure_plate, new BlockEntry(PressurePlateFactor, PressurePlateFactor, false, false, false));
		
		final double ThinPaneFactor = 0.2;
		addScaledEntry(Blocks.glass_pane, new BlockEntry(ThinPaneFactor, ThinPaneFactor, true, false, false));
		
		final double SignFactor = 0.2;
		addScaledEntry(Blocks.wall_sign, new BlockEntry(SignFactor, SignFactor, false, false, false));
		
		// special ships mod blocks
		addEntry(Ships.m_blockAirWall, new BlockEntry(0, 0, false, true, true));
		addEntry(Ships.m_blockAirRoof, new BlockEntry(0, 0, false, true, false));
	}
	
	public static void readConfigFile()
	throws FileNotFoundException {
		File inFile = new File("config/shipBlockProperties.cfg");
		if (!inFile.exists()) {
			Ships.logger.info("Skipping ships block config. No config file found at: " + inFile.getAbsolutePath());
			return;
		}
		
		m_overriddenEntries.clear();
		readEntries(m_overriddenEntries, new FileReader(inFile));
		Ships.logger.info("Read %d block properties from: %s", m_overriddenEntries.size(), inFile.getAbsolutePath());
	}
	
	public static void setOverrides(String in) {
		m_overriddenEntries.clear();
		readEntries(m_overriddenEntries, new StringReader(in));
	}
	
	public static String getOverrides() {
		return writeEntries(m_overriddenEntries);
	}
	
	public static boolean hasOverrides() {
		return !m_overriddenEntries.isEmpty();
	}
	
	public static boolean isOverridden(Block block) {
		return m_overriddenEntries.containsKey(block);
	}
	
	public static Iterable<Map.Entry<Block,BlockEntry>> overrides() {
		return m_overriddenEntries.entrySet();
	}
	
	public static void addScaledEntry(Block block, BlockEntry entry) {
		// scale the default mass by the scale factor
		double scaleFactor = entry.mass;
		double defaultMass = DefaultBlockProperties.getEntry(block).mass;
		entry.mass = defaultMass * scaleFactor;
		
		m_hardcodedEntries.put(block, entry);
	}
	
	public static void addEntry(Block block, BlockEntry entry) {
		m_hardcodedEntries.put(block, entry);
	}
	
	public static double getMass(Block block) {
		return getEntry(block).mass;
	}
	
	public static double getDisplacement(Block block) {
		return getEntry(block).displacement;
	}
	
	public static boolean isWatertight(Block block) {
		return getEntry(block).isWatertight;
	}
	
	public static boolean isSeparator(Block block) {
		return getEntry(block).isSeparator;
	}
	
	public static boolean isWater(Block block) {
		return getEntry(block).isWater;
	}
	
	public static BlockEntry getEntry(Block block) {
		BlockEntry entry = null;
		
		// first, check the overrides
		entry = m_overriddenEntries.get(block);
		if (entry != null) {
			return entry;
		}
		
		// then, check the hard-coded entries
		entry = m_hardcodedEntries.get(block);
		if (entry != null) {
			return entry;
		}
		
		// finally, rely on the defaults
		return DefaultBlockProperties.getEntry(block);
	}
	
	@SuppressWarnings("unchecked")
	private static void readEntries(Map<Block,BlockEntry> entries, Reader inRaw) {
		
		// build a map of the block names
		Map<String,Block> blocks = new HashMap<String,Block>();
		for (Block block : (Iterable<Block>)Block.blockRegistry) {
			if (block != null) {
				blocks.put(Block.blockRegistry.getNameForObject(block), block);
			}
		}
		
		try {
			// open the file for reading line-by-line
			BufferedReader in = new BufferedReader(inRaw);
			String line = null;
			while ((line = in.readLine()) != null) {
				
				// TEMP
				System.out.println("LINE: " + line);
				
				// skip blank or empty lines
				line = line.trim();
				if (line.length() <= 0) {
					continue;
				}
				
				try {
					// read the block and the entry
					Block block = null;
					String[] parts = line.split("=");
					if (parts.length != 2) {
						throw new IllegalArgumentException();
					}
					block = blocks.get(parts[0]);
					if (block == null) {
						throw new IllegalArgumentException("Unknown block id name: " + parts[0]);
					}
					BlockEntry entry = readEntry(parts[1]);
					
					// save the entry
					if (block != null && entry != null) {
						entries.put(block, entry);
					}
				} catch (RuntimeException ex) {
					Ships.logger.warning(ex, "Malformed block entry: %s", line);
				}
			}
			in.close();
		} catch (IOException ex) {
			Ships.logger.warning(ex, "Unable to read block properties!");
		}
	}
	
	private static BlockEntry readEntry(String entryString) {
		String[] parts = entryString.split(";");
		if (parts.length != 5) {
			throw new IllegalArgumentException();
		}
		
		return new BlockEntry(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Boolean.parseBoolean(parts[2]), Boolean.parseBoolean(parts[3]), Boolean.parseBoolean(parts[4]));
	}
	
	private static String writeEntries(Map<Block,BlockEntry> entries) {
		StringBuilder buf = new StringBuilder();
		for (Map.Entry<Block,BlockEntry> entry : entries.entrySet()) {
			buf.append(Block.blockRegistry.getNameForObject(entry.getKey()));
			buf.append("=");
			buf.append(entry.getValue().mass);
			buf.append(";");
			buf.append(entry.getValue().displacement);
			buf.append(";");
			buf.append(entry.getValue().isWatertight);
			buf.append(";");
			buf.append(entry.getValue().isSeparator);
			buf.append(";");
			buf.append(entry.getValue().isWater);
			buf.append("\n");
		}
		return buf.toString();
	}
}
