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

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.TileEntityHelm;
import cuchaz.ships.gui.Gui;

public class BlockHelm extends Block {
	
	public BlockHelm() {
		super(Material.wood);
		
		setHardness(2.0F);
		setResistance(5.0F);
		setStepSound(soundTypeWood);
		setBlockName("cuchaz.ships.helm");
		setCreativeTab(CreativeTabs.tabTransport);
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
	
	@Override
	public int getRenderType() {
		return -1;
	}
	
	@Override
	public boolean hasTileEntity(int metadata) {
		return true;
	}
	
	@Override
	public TileEntity createTileEntity(World world, int metadata) {
		return new TileEntityHelm();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister) {
		this.blockIcon = iconRegister.registerIcon("ships:helm");
	}
	
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityUser, ItemStack itemStack) {
		// save the block rotation to the metadata
		world.setBlockMetadataWithNotify(x, y, z, BlockSide.getByYaw(entityUser.rotationYaw).getXZOffset(), 3);
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float xOffset, float yOffset, float zOffset) {
		// are we in the world, or on the ship?
		if (world instanceof ShipWorld) {
			ShipWorld shipWorld = (ShipWorld)world;
			if (shipWorld.getShipType().isPaddleable()) {
				// disable the helm
			} else {
				Gui.PilotSurfaceShip.open(player, world, x, y, z);
			}
		} else {
			Gui.ShipPropulsion.open(player, world, x, y, z);
		}
		return true;
	}
}
