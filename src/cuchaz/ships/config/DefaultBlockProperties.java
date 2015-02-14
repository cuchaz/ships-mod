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
package cuchaz.ships.config;

import java.lang.reflect.Field;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import cuchaz.ships.Ships;

public class DefaultBlockProperties {
	
	private static HashMap<Material,BlockEntry> m_properties;
	private static final BlockEntry DefaultProperties;
	
	static {
		m_properties = new HashMap<Material,BlockEntry>();
		
		// NOTE: sadly, we can't use hardness or resistance as a proxy for mass/density.
		// btw, mass is a proxy for density since all block volumes are the same
		
		// all masses are normalized to water, which has a mass of 1
		m_properties.put(Material.water, new BlockEntry(1.0, 1.0, false, true, true));
		m_properties.put(Material.lava, new BlockEntry(2.0, 1.0, false, true, false));
		
		m_properties.put(Material.air, new BlockEntry(0.01, 1.0, false, true, false));
		
		// solids
		m_properties.put(Material.wood, new BlockEntry(0.5, 1.0, true, false, false));
		m_properties.put(Material.rock, new BlockEntry(3.0, 1.0, true, false, false));
		m_properties.put(Material.iron, new BlockEntry(4.0, 1.0, true, false, false));
		m_properties.put(Material.glass, new BlockEntry(0.6, 1.0, true, false, false));
		m_properties.put(Material.anvil, new BlockEntry(6.0, 1.0, true, false, false));
		m_properties.put(Material.ice, new BlockEntry(0.8, 1.0, true, false, false));
		m_properties.put(Material.packedIce, new BlockEntry(0.8, 1.0, true, false, false));
		m_properties.put(Material.clay, new BlockEntry(3.0, 1.0, true, false, false));
		
		// particulates
		m_properties.put(Material.ground, new BlockEntry(2.0, 1.0, false, false, false));
		m_properties.put(Material.grass, new BlockEntry(2.0, 1.0, false, false, false));
		m_properties.put(Material.sand, new BlockEntry(2.0, 1.0, false, false, false));
		m_properties.put(Material.snow, new BlockEntry(0.6, 1.0, false, false, false));
		m_properties.put(Material.craftedSnow, new BlockEntry(0.6, 1.0, false, false, false));
		
		// porous
		m_properties.put(Material.cloth, new BlockEntry(0.2, 1.0, false, false, false));
		m_properties.put(Material.carpet, new BlockEntry(0.2, 1.0, false, false, false));
		m_properties.put(Material.web, new BlockEntry(0.1, 1.0, false, false, false));
		m_properties.put(Material.coral, new BlockEntry(2.0, 1.0, false, false, false));
		m_properties.put(Material.sponge, new BlockEntry(0.2, 1.0, false, false, false));
		
		// vegetation/food
		m_properties.put(Material.leaves, new BlockEntry(0.5, 1.0, false, false, false));
		m_properties.put(Material.plants, new BlockEntry(0.5, 1.0, false, false, false));
		m_properties.put(Material.vine, new BlockEntry(0.5, 1.0, false, false, false));
		m_properties.put(Material.cactus, new BlockEntry(0.5, 1.0, false, false, false));
		m_properties.put(Material.gourd, new BlockEntry(0.5, 1.0, false, false, false));
		m_properties.put(Material.cake, new BlockEntry(0.5, 1.0, false, false, false));
		m_properties.put(Material.dragonEgg, new BlockEntry(1.2, 1.0, false, false, false));
		
		// machines
		m_properties.put(Material.circuits, new BlockEntry(2.0, 1.0, false, false, false));
		m_properties.put(Material.redstoneLight, new BlockEntry(2.0, 1.0, false, false, false));
		m_properties.put(Material.tnt, new BlockEntry(2.0, 1.0, false, false, false));
		m_properties.put(Material.piston, new BlockEntry(2.0, 1.0, false, false, false));
		
		// other
		m_properties.put(Material.portal, new BlockEntry(2.0, 1.0, false, true, false));
		m_properties.put(Material.fire, new BlockEntry(0.0, 1.0, false, true, false));
		
		// for unknown materials, assume same as water, but not water
		DefaultProperties = new BlockEntry(1.0, 1.0, false, true, false);
		
		// just for fun, make sure we have all the materials covered
		// since Material isn't an enum, we have to use reflection here
		for (Field field : Material.class.getDeclaredFields()) {
			if (field.getType() == Material.class) {
				try {
					Material material = (Material)field.get(null);
					if (m_properties.get(material) == null) {
						Ships.logger.warning("Material %s is not configured!", field.getName());
					}
				} catch (Exception ex) {
					Ships.logger.warning(ex, "Unable to read material: %s", field.getName());
				}
			}
		}
	}
	
	public static BlockEntry getEntry(Block block) {
		BlockEntry entry = null;
		if (block != null) {
			entry = m_properties.get(block.getMaterial());
		}
		if (entry == null) {
			entry = DefaultProperties;
		}
		return entry;
	}
}
