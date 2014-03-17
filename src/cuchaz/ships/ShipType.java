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

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum ShipType
{
	Tiny( 20, true )
	{
		@Override
		protected void registerBlock( )
		{
			LanguageRegistry.addName( newItemStack(), "Tiny Ship" );
			GameRegistry.addRecipe(
				newItemStack(),
				"xxx", "xyx", "xxx",
				'x', new ItemStack( Item.ingotIron ),
				'y', new ItemStack( Item.redstone )
			);
		}
	},
	Small( 200, false )
	{
		@Override
		protected void registerBlock( )
		{
			LanguageRegistry.addName( newItemStack(), "Small Ship" );
			GameRegistry.addRecipe(
				newItemStack(),
				"xzx", "zyz", "xzx",
				'x', new ItemStack( Item.ingotIron ),
				'y', new ItemStack( Item.redstone ),
				'z', new ItemStack( Item.ingotGold )
			);
		}
	},
	Medium( 400, false )
	{
		@Override
		protected void registerBlock( )
		{
			LanguageRegistry.addName( newItemStack(), "Medium Ship" );
			GameRegistry.addRecipe(
				newItemStack(),
				"xxx", "xyx", "xxx",
				'x', new ItemStack( Item.ingotGold ),
				'y', new ItemStack( Item.redstone )
			);
		}
	},
	Large( 1000, false )
	{
		@Override
		protected void registerBlock( )
		{
			LanguageRegistry.addName( newItemStack(), "Large Ship" );
			GameRegistry.addRecipe(
				newItemStack(),
				"xzx", "zyz", "xzx",
				'x', new ItemStack( Item.ingotGold ),
				'y', new ItemStack( Item.redstone ),
				'z', new ItemStack( Item.dyePowder, 1, 4 ) // lapis lazuli
			);
		}
	};
	
	private int m_maxNumBlocks;
	private boolean m_isPaddleable;
	
	@SideOnly( Side.CLIENT )
	private Icon m_icon;
	
	private ShipType( int maxNumBlocks, boolean isPaddleable )
	{
		m_maxNumBlocks = maxNumBlocks;
		m_isPaddleable = isPaddleable;
	}
	
	public int getMaxNumBlocks( )
	{
		return m_maxNumBlocks;
	}
	
	public boolean isPaddleable( )
	{
		return m_isPaddleable;
	}
	
	@SideOnly( Side.CLIENT )
	public static void registerIcons( IconRegister iconRegister )
	{
		for( ShipType type : values() )
		{
			type.m_icon = iconRegister.registerIcon( "ships:shipSide-" + type.name().toLowerCase() );
		}
	}
	
	public Icon getIcon( )
	{
		return m_icon;
	}
	
	public static ShipType getByMeta( int meta )
	{
		return values()[meta];
	}
	
	public int getMeta( )
	{
		return ordinal();
	}
	
	public ItemStack newItemStack( )
	{
		return new ItemStack( Ships.m_blockShip, 1, getMeta() );
	}
	
	public static void registerBlocks( )
	{
		for( ShipType type : values() )
		{
			type.registerBlock();
		}
	}
	
	protected abstract void registerBlock( );
}
