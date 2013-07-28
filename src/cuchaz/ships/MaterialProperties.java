package cuchaz.ships;

import java.util.HashMap;

import net.minecraft.block.Block;
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
	}
	
	public static double getMass( Block block )
	{
		if( block == null )
		{
			return 0;
		}
		return m_properties.get( block.blockMaterial ).mass;
	}
	
	public static boolean isWatertight( Block block )
	{
		if( block == null )
		{
			return false;
		}
		return m_properties.get( block.blockMaterial ).isWatertight;
	}
	
	public static boolean isSeparatorBlock( Block block )
	{
		if( block == null )
		{
			return true;
		}
		
		return block.blockMaterial.isLiquid() || block.blockMaterial == Material.fire || block.blockMaterial == Ships.MaterialAirWall;
		
		// UNDONE: add dock blocks
	}
}
