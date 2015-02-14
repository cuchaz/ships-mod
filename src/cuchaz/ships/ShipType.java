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

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum ShipType {
	Tiny(20, true) {
		
		@Override
		protected void registerBlock() {
			GameRegistry.addRecipe(newItemStack(),
				"xxx",
				"xyx",
				"xxx",
				'x', new ItemStack(Items.iron_ingot),
				'y', new ItemStack(Items.redstone)
			);
		}
	},
	Small(200, false) {
		
		@Override
		protected void registerBlock() {
			GameRegistry.addRecipe(newItemStack(),
				"xzx",
				"zyz",
				"xzx",
				'x', new ItemStack(Items.iron_ingot),
				'y', new ItemStack(Items.redstone),
				'z', new ItemStack(Items.gold_ingot)
			);
		}
	},
	Medium(400, false) {
		
		@Override
		protected void registerBlock() {
			GameRegistry.addRecipe(newItemStack(),
				"xxx",
				"xyx",
				"xxx",
				'x', new ItemStack(Items.gold_ingot),
				'y', new ItemStack(Items.redstone)
			);
		}
	},
	Large(1000, false) {
		
		@Override
		protected void registerBlock() {
			GameRegistry.addRecipe(newItemStack(),
				"xzx",
				"zyz",
				"xzx",
				'x', new ItemStack(Items.gold_ingot),
				'y', new ItemStack(Items.redstone),
				'z', new ItemStack(Items.dye, 1, 4) // lapis lazuli
			);
		}
	},
	Huge(2000, false) {
		
		@Override
		protected void registerBlock() {
			GameRegistry.addRecipe(newItemStack(),
				"xxx",
				"xyx",
				"xxx",
				'x', new ItemStack(Items.dye, 1, 4), // lapis lazuli
				'y', new ItemStack(Items.redstone)
			);
		}
	},
	Gigantic(4000, false) {
		
		@Override
		protected void registerBlock() {
			GameRegistry.addRecipe(newItemStack(),
				"xzx",
				"zyz",
				"xzx",
				'x', new ItemStack(Items.dye, 1, 4), // lapis lazuli
				'y', new ItemStack(Items.redstone),
				'z', new ItemStack(Items.diamond)
			);
		}
	},
	Epic(10000, false) {
		
		@Override
		protected void registerBlock() {
			GameRegistry.addRecipe(newItemStack(),
				"xxx",
				"xyx",
				"xxx",
				'x', new ItemStack(Items.diamond),
				'y', new ItemStack(Items.redstone)
			);
		}
	};
	
	private int m_maxNumBlocks;
	private boolean m_isPaddleable;
	
	@SideOnly(Side.CLIENT)
	private IIcon m_icon;
	
	private ShipType(int maxNumBlocks, boolean isPaddleable) {
		m_maxNumBlocks = maxNumBlocks;
		m_isPaddleable = isPaddleable;
	}
	
	public int getMaxNumBlocks() {
		return m_maxNumBlocks;
	}
	
	public boolean isPaddleable() {
		return m_isPaddleable;
	}
	
	@SideOnly(Side.CLIENT)
	public static void registerIcons(IIconRegister iconRegister) {
		for (ShipType type : values()) {
			type.m_icon = iconRegister.registerIcon("ships:shipSide-" + type.name().toLowerCase());
		}
	}
	
	public IIcon getIcon() {
		return m_icon;
	}
	
	public static ShipType getByMeta(int meta) {
		return values()[meta];
	}
	
	public int getMeta() {
		return ordinal();
	}
	
	public ItemStack newItemStack() {
		return new ItemStack(Ships.m_blockShip, 1, getMeta());
	}
	
	public static void registerRecipes() {
		for (ShipType type : values()) {
			type.registerBlock();
		}
	}
	
	protected abstract void registerBlock();
}
