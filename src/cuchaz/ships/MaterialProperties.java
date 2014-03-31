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

import java.lang.reflect.Field;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockHalfSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;

public class MaterialProperties
{
	private static class Entry
	{
		public double mass;
		public boolean isWatertight;
		
		public Entry( double mass, boolean isWatertight )
		{
			this.mass = mass;
			this.isWatertight = isWatertight;
		}
	}
	
	private static HashMap<Material,Entry> m_properties;
	private static final Entry DefaultProperties;
	
	static
	{
		m_properties = new HashMap<Material,Entry>();
		
		// NOTE: sadly, we can't use hardness or resistance as a proxy for mass/density.
		// btw, mass is a proxy for density since all block volumes are the same
		
		// all masses are normalized to water, which has a mass of 1
		m_properties.put( Material.water, new Entry( 1.0, false ) );
	    m_properties.put( Material.lava, new Entry( 2.0, false ) );
		
		m_properties.put( Material.air, new Entry( 0.01, false ) );
		
		// solids
		m_properties.put( Material.wood, new Entry( 0.5, true ) );
		m_properties.put( Material.rock, new Entry( 3.0, true ) );
		m_properties.put( Material.iron, new Entry( 4.0, true ) );
		m_properties.put( Material.glass, new Entry( 0.6, true ) );
		m_properties.put( Material.anvil, new Entry( 6.0, true ) );
	    m_properties.put( Material.ice, new Entry( 0.8, true ) );
	    m_properties.put( Material.clay, new Entry( 3.0, true ) );
		
		// particulates
		m_properties.put( Material.ground, new Entry( 2.0, false ) );
		m_properties.put( Material.grass, new Entry( 2.0, false ) );
	    m_properties.put( Material.sand, new Entry( 2.0, false ) );
	    m_properties.put( Material.snow, new Entry( 0.6, false ) );
	    m_properties.put( Material.craftedSnow, new Entry( 0.6, false ) );
	    
	    // porous
		m_properties.put( Material.cloth, new Entry( 0.2, false ) );
		m_properties.put( Material.materialCarpet, new Entry( 0.2, false ) );
	    m_properties.put( Material.web, new Entry( 0.1, false ) );
	    m_properties.put( Material.coral, new Entry( 2.0, false ) );
	    m_properties.put( Material.sponge, new Entry( 0.2, false ) );
		
		// vegetation/food
	    m_properties.put( Material.leaves, new Entry( 0.5, false ) );
	    m_properties.put( Material.plants, new Entry( 0.5, false ) );
	    m_properties.put( Material.vine, new Entry( 0.5, false ) );
	    m_properties.put( Material.cactus, new Entry( 0.5, false ) );
	    m_properties.put( Material.pumpkin, new Entry( 0.5, false ) );
	    m_properties.put( Material.cake, new Entry( 0.5, false ) );
	    m_properties.put( Material.dragonEgg, new Entry( 1.2, false ) );
	    
	    // machines
	    m_properties.put( Material.circuits, new Entry( 2.0, false ) );
	    m_properties.put( Material.redstoneLight, new Entry( 2.0, false ) );
	    m_properties.put( Material.tnt, new Entry( 2.0, false ) );
	    m_properties.put( Material.piston, new Entry( 2.0, false ) );
	    m_properties.put( Material.portal, new Entry( 2.0, false ) );
	    
	    // other
	    m_properties.put( Material.fire, new Entry( 0.0, false ) );
	    
	    // if the material is unknown, assume it's the same as water
	    DefaultProperties = new Entry( 1.0, false );
	    
	    // just for fun, make sure we have all the materials covered
	    // since Material isn't an enum, we have to use reflection here
	    for( Field field : Material.class.getDeclaredFields() )
	    {
	    	if( field.getType() == Material.class )
	    	{
	    		try
				{
					Material material = (Material)field.get( null );
					if( m_properties.get( material ) == null )
					{
						Ships.logger.warning( "Material %s is not configured!", field.getName() );
					}
				}
				catch( Exception ex )
				{
					Ships.logger.warning( ex, "Unable to read material: %s", field.getName() );
				}
	    	}
	    }
	}
	
	public static double getMass( Block block )
	{
		if( block == null )
		{
			return 0;
		}
		return getSizeMultiplier( block )*getEntry( block ).mass;
	}
	
	public static boolean isWatertight( Block block )
	{
		if( block == null )
		{
			return false;
		}
		return getEntry( block ).isWatertight;
	}
	
	public static double getDisplacement( Block block )
	{
		return getSizeMultiplier( block );
	}
	
	public static boolean isSeparatorBlock( Block block )
	{
		if( block == null )
		{
			return true;
		}
		
		return !block.blockMaterial.isSolid() && block.blockMaterial.isReplaceable();
		// this should include: liquid blocks, fire blocks, air blocks
		// this should exclude: solid blocks, circuits
		
		// UNDONE: add dock blocks
	}
	
	private static Entry getEntry( Block block )
	{
		Entry entry = m_properties.get( block.blockMaterial );
		if( entry == null )
		{
			entry = DefaultProperties;
		}
		return entry;
	}
	
	private static double getSizeMultiplier( Block block )
	{
		if( block == null )
		{
			return 1.0;
		}
		
		// handle stairs, slabs, etc
		if( block instanceof BlockHalfSlab )
		{
			// 1 means half slab, 2 means double slab
			if( ((BlockHalfSlab)block).quantityDropped( null ) == 1 )
			{
				return 0.5;
			}
		}
		else if( block instanceof BlockStairs )
		{
			return 0.75;
		}
		else if( block.isLadder( null, 0, 0, 0, null ) )
		{
			return 0.1;
		}
		else if( block instanceof BlockFence )
		{
			return 0.5;
		}
		
		return 1.0;
	}
}
