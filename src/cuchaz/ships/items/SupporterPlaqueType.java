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
package cuchaz.ships.items;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.registry.GameRegistry;
import cuchaz.ships.ShipType;
import cuchaz.ships.Ships;
import cuchaz.ships.Supporters;

public enum SupporterPlaqueType
{
	Small( 16, 2, "Cuchaz Interactive Supporter Plaque" )
	{
		@Override
		public void registerRecipe( )
		{
			GameRegistry.addRecipe(
				newItemStack(),
				" y ", " x ", "zzz",
				'x', ShipType.Tiny.newItemStack(),
				'y', new ItemStack( Item.ingotIron ),
				'z', new ItemStack( Block.planks )
			);
		}
	},
	Large( 32, 3, "Cuchaz Interactive Supporter Plaque (Large)" )
	{
		@Override
		public void registerRecipe( )
		{
			GameRegistry.addRecipe(
				newItemStack(),
				"yyy", "zxz", "zzz",
				'x', ShipType.Tiny.newItemStack(),
				'y', new ItemStack( Item.ingotIron ),
				'z', new ItemStack( Block.planks )
			);
		}
	};
	
	private int m_size;
	private int m_minRank;
	private String m_itemName;
	private ResourceLocation m_texture;
	
	private SupporterPlaqueType( int size, int minRank, String itemName )
	{
		m_size = size;
		m_minRank = minRank;
		m_itemName = itemName;
		m_texture = new ResourceLocation( "ships", String.format( "textures/blocks/supporterPlaque-%s.png", name().toLowerCase() ) );
	}
	
	public int getSize( )
	{
		return m_size;
	}
	
	public int getMinRank( )
	{
		return m_minRank;
	}
	
	public String getItemName( )
	{
		return m_itemName;
	}
	
	public ResourceLocation getTexture( )
	{
		return m_texture;
	}
	
	public int getMeta( )
	{
		return ordinal();
	}
	
	public static SupporterPlaqueType getByMeta( int meta )
	{
		return values()[meta];
	}
	
	public ItemStack newItemStack( )
	{
		return new ItemStack( Ships.m_itemSupporterPlaque, 1, getMeta() );
	}
	
	public static void registerRecipes( )
	{
		for( SupporterPlaqueType type : values() )
		{
			type.registerRecipe();
		}
	}
	
	public boolean canUse( EntityPlayer player )
	{
		// is this player a supporter?
		int supporterId = Supporters.getId( player.username );
		if( supporterId != Supporters.InvalidSupporterId )
		{
			// does the player meet the min rank?
			if( Supporters.getRank( supporterId ) >= m_minRank )
			{
				return true;
			}
		}
		
		return false;
	}
	
	public abstract void registerRecipe( );
}
