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
package cuchaz.ships.items;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.ships.Ships;

public class ItemMagicBucket extends Item {
	
	public ItemMagicBucket() {
		maxStackSize = 1;
		setCreativeTab(CreativeTabs.tabTools);
		setUnlocalizedName("cuchaz.ships.magicBucket");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconRegister) {
		itemIcon = iconRegister.registerIcon("ships:magicBucket");
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
		// find out where we're aiming
		final boolean IntersectWater = true;
		MovingObjectPosition movingobjectposition = getMovingObjectPositionFromPlayer(world, player, IntersectWater);
		if (movingobjectposition == null || movingobjectposition.typeOfHit != MovingObjectType.BLOCK) {
			return itemStack;
		}
		
		int x = movingobjectposition.blockX;
		int y = movingobjectposition.blockY;
		int z = movingobjectposition.blockZ;
		
		if (!world.canMineBlock(player, x, y, z)) {
			return itemStack;
		}
		if (!player.canPlayerEdit(x, y, z, movingobjectposition.sideHit, itemStack)) {
			return itemStack;
		}
		
		// is it a liquid block?
		Material material = world.getBlock(x, y, z).getMaterial();
		if ( (material == Material.water || material == Material.lava) && world.getBlockMetadata(x, y, z) == 0) {
			// make it an air wall!
			world.setBlock(x, y, z, Ships.m_blockAirWall);
		}
		// is it an air wall?
		else if (material == Ships.m_materialAirWall) {
			// make it back to normal air!
			world.setBlockToAir(x, y, z);
		}
		
		return itemStack;
	}
}
