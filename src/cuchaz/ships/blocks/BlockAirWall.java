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

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.ships.Ships;

public class BlockAirWall extends Block {
	
	public BlockAirWall() {
		// an air block that stops flowing water
		super(Ships.m_materialAirWall);
		
		setBlockName("cuchaz.ships.airWall");
		setBlockBounds(0, 0, 0, 1, 1, 1);
	}
	
	@Override
	public int getRenderType() {
		// no really, don't EVER render this block
		return -1;
	}
	
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return null;
	}
	
	@Override
	public boolean canCollideCheck(int meta, boolean hitLiquids) {
		return hitLiquids;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
		return false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister) {
		// do nothing
	}
	
	@Override
	public int quantityDropped(Random rand) {
		return 0;
	}
	
	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return null;
	}
	
	@Override
	public boolean canHarvestBlock(EntityPlayer player, int meta) {
		return false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean addDestroyEffects(World world, int x, int y, int z, int meta, EffectRenderer effectRenderer) {
		// don't show block break effects
		return true;
	}
}
