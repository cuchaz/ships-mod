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
package cuchaz.ships.blocks;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.ships.ShipType;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;
import cuchaz.ships.gui.Gui;

public class BlockShip extends Block {
	
	public BlockShip() {
		super(Material.iron);
		
		setHardness(5.0F);
		setResistance(10.0F);
		setStepSound(soundTypeMetal);
		setBlockName("cuchaz.ships.blockShip");
		setCreativeTab(CreativeTabs.tabTransport);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		switch (BlockSide.getById(side)) {
			case North:
			case South:
			case East:
			case West:
				return ShipType.getByMeta(meta).getIcon();
				
			default:
				return blockIcon;
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister) {
		blockIcon = iconRegister.registerIcon("ships:shipTop");
		ShipType.registerIcons(iconRegister);
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float xOffset, float yOffset, float zOffset) {
		// are we in the world, or on the ship?
		if (world instanceof ShipWorld) {
			// can the player paddle this ship?
			boolean isPaddleEquipped = player.getCurrentEquippedItem() != null && player.getCurrentEquippedItem().getItem() == Ships.m_itemPaddle;
			ShipWorld shipWorld = (ShipWorld)world;
			if (isPaddleEquipped && shipWorld.getShipType().isPaddleable() && shipWorld.getShip().getCollider().isEntityAboard(player)) {
				Gui.PaddleShip.open(player, world, x, y, z);
			} else {
				Gui.UnbuildShip.open(player, world, x, y, z);
			}
		} else {
			Gui.BuildShip.open(player, world, x, y, z);
		}
		return true;
	}
	
	public ShipType getShipType(World world, int x, int y, int z) {
		return ShipType.getByMeta(world.getBlockMetadata(x, y, z));
	}
	
	@Override
	public int damageDropped(int metadata) {
		return metadata;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void getSubBlocks(Item item, CreativeTabs tabs, List subItems) {
		for (ShipType type : ShipType.values()) {
			subItems.add(type.newItemStack());
		}
	}
}
